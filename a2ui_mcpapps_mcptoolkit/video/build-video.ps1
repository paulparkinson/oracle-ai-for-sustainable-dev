[CmdletBinding()]
param(
    [ValidateRange(8, 25)]
    [int]$SecondsPerSlide = 14
)

$ErrorActionPreference = "Stop"
$videoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $videoRoot
$imagesRoot = Join-Path $projectRoot "images"
$buildRoot = Join-Path $videoRoot ".build"
$videoPath = Join-Path $videoRoot "interactive-ai-walkthrough.mp4"
$posterPath = Join-Path $videoRoot "interactive-ai-walkthrough-poster.png"
$srtPath = Join-Path $videoRoot "interactive-ai-walkthrough.srt"
$vttPath = Join-Path $videoRoot "interactive-ai-walkthrough.vtt"
$deckPath = Join-Path $buildRoot "interactive-ai-walkthrough.pptx"

New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null
foreach ($path in @($videoPath, $posterPath, $srtPath, $vttPath, $deckPath)) {
    if (Test-Path -LiteralPath $path) {
        Remove-Item -LiteralPath $path -Force
    }
}

$scenes = @(
    [pscustomobject]@{ Narration = "01-intro.txt"; Eyebrow = "THREE HOST MODELS"; Title = "One governed supply-chain workflow"; Image = $null },
    [pscustomobject]@{ Narration = "02-architecture.txt"; Eyebrow = "REFERENCE ARCHITECTURE"; Title = "Host adapters, one governed core"; Image = "interactive-ai-architecture.svg" },
    [pscustomobject]@{ Narration = "03-mcp-runtime.txt"; Eyebrow = "ORACLE DATABASE MCP JAVA TOOLKIT"; Title = "Purpose-built tools, not unrestricted SQL"; Image = "protocol-responsibility-map.svg" },
    [pscustomobject]@{ Narration = "04-mcp-tools.txt"; Eyebrow = "GEMINI ENTERPRISE"; Title = "Register the A2A custom agent"; Image = "gemini-enterprise-agent-registration.png" },
    [pscustomobject]@{ Narration = "05-database.txt"; Eyebrow = "GEMINI ENTERPRISE + A2UI"; Title = "Render native governed controls"; Image = "gemini-enterprise-a2ui-review.png" },
    [pscustomobject]@{ Narration = "06-agui.txt"; Eyebrow = "CHATGPT + MCP APPS"; Title = "Discover one bounded dashboard tool"; Image = "chatgpt-plugin-tool-discovery.png" },
    [pscustomobject]@{ Narration = "07-a2ui.txt"; Eyebrow = "CHATGPT + MCP APPS"; Title = "Render the live Oracle-backed dashboard"; Image = "chatgpt-mcp-app-dashboard.png" },
    [pscustomobject]@{ Narration = "08-mcp-app.txt"; Eyebrow = "CLAUDE + MCP APPS"; Title = "Connect the same interactive MCP server"; Image = "claude-connector-tool-permissions.png" },
    [pscustomobject]@{ Narration = "09-application.txt"; Eyebrow = "CLAUDE + MCP APPS"; Title = "Render the same live dashboard"; Image = "claude-mcp-app-dashboard.png" },
    [pscustomobject]@{ Narration = "10-approval.txt"; Eyebrow = "HOST COMPARISON"; Title = "Native A2UI or sandboxed MCP Apps"; Image = "interactive-ai-architecture.svg" },
    [pscustomobject]@{ Narration = "11-recap.txt"; Eyebrow = "PRODUCTION BOUNDARY"; Title = "Read-only validation before authenticated action"; Image = $null }
)

function Get-Rgb([int]$red, [int]$green, [int]$blue) {
    return $red + (256 * $green) + (65536 * $blue)
}

function New-ZoomStrip {
    param([string]$Source, [string]$Destination, [double[]]$Centers)
    Add-Type -AssemblyName System.Drawing
    $sourceImage = [Drawing.Bitmap]::FromFile($Source)
    $canvas = New-Object Drawing.Bitmap 1680, 560
    $graphics = [Drawing.Graphics]::FromImage($canvas)
    try {
        $graphics.Clear([Drawing.Color]::FromArgb(35, 39, 43))
        $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $cropHeight = [int]($sourceImage.Height * 0.24)
        $cropWidth = [int]($cropHeight * 1.5)
        if ($cropWidth -gt $sourceImage.Width) {
            $cropWidth = $sourceImage.Width
            $cropHeight = [int]($cropWidth / 1.5)
        }
        for ($index = 0; $index -lt 2; $index++) {
            $sourceX = if ($index -eq 0) {
                0
            }
            else {
                [Math]::Max(0, $sourceImage.Width - $cropWidth)
            }
            $centerY = [int]($sourceImage.Height * $Centers[$index])
            $sourceY = [Math]::Max(0, [Math]::Min(
                $sourceImage.Height - $cropHeight,
                $centerY - [int]($cropHeight / 2)))
            $destinationRectangle = New-Object Drawing.Rectangle (($index * 850) + 5), 20, 820, 520
            $sourceRectangle = New-Object Drawing.Rectangle $sourceX, $sourceY, $cropWidth, $cropHeight
            $graphics.DrawImage($sourceImage, $destinationRectangle, $sourceRectangle, [Drawing.GraphicsUnit]::Pixel)
        }
        $canvas.Save($Destination, [Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $graphics.Dispose()
        $canvas.Dispose()
        $sourceImage.Dispose()
    }
}

function Add-TextBox {
    param($Slide, [string]$Text, [double]$Left, [double]$Top,
        [double]$Width, [double]$Height, [double]$FontSize,
        [int]$Color, [bool]$Bold = $false, [int]$Alignment = 1)
    $shape = $Slide.Shapes.AddTextbox(1, $Left, $Top, $Width, $Height)
    $shape.TextFrame.MarginLeft = 0
    $shape.TextFrame.MarginRight = 0
    $shape.TextFrame.MarginTop = 0
    $shape.TextFrame.MarginBottom = 0
    $shape.TextFrame.WordWrap = -1
    $shape.TextFrame.TextRange.Text = $Text
    $shape.TextFrame.TextRange.Font.Name = "Aptos"
    $shape.TextFrame.TextRange.Font.Size = $FontSize
    $shape.TextFrame.TextRange.Font.Color.RGB = $Color
    $shape.TextFrame.TextRange.Font.Bold = if ($Bold) { -1 } else { 0 }
    $shape.TextFrame.TextRange.ParagraphFormat.Alignment = $Alignment
    return $shape
}

function Add-FittedPicture {
    param($Slide, [string]$Path, [double]$Left, [double]$Top,
        [double]$Width, [double]$Height)
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Missing video visual: $Path"
    }
    $picture = $Slide.Shapes.AddPicture($Path, 0, -1, 0, 0, -1, -1)
    $picture.LockAspectRatio = -1
    $scale = [Math]::Min($Width / $picture.Width, $Height / $picture.Height)
    $picture.Width = $picture.Width * $scale
    $picture.Height = $picture.Height * $scale
    $picture.Left = $Left + (($Width - $picture.Width) / 2)
    $picture.Top = $Top + (($Height - $picture.Height) / 2)
    return $picture
}

$geminiStrip = Join-Path $buildRoot "gemini-enterprise-live-strip.png"
$chatGptStrip = Join-Path $buildRoot "chatgpt-live-strip.png"
$claudeStrip = Join-Path $buildRoot "claude-live-strip.png"
New-ZoomStrip (Join-Path $imagesRoot "gemini-enterprise-a2ui-review.png") $geminiStrip @(0.27, 0.55)
New-ZoomStrip (Join-Path $imagesRoot "chatgpt-mcp-app-dashboard.png") $chatGptStrip @(0.30, 0.55)
New-ZoomStrip (Join-Path $imagesRoot "claude-mcp-app-dashboard.png") $claudeStrip @(0.30, 0.56)
$scenes[4].Image = $geminiStrip
$scenes[6].Image = $chatGptStrip
$scenes[8].Image = $claudeStrip

function Format-Time([double]$Seconds, [string]$Separator) {
    $milliseconds = [int][Math]::Round($Seconds * 1000)
    $hours = [Math]::Floor($milliseconds / 3600000)
    $minutes = [Math]::Floor(($milliseconds % 3600000) / 60000)
    $wholeSeconds = [Math]::Floor(($milliseconds % 60000) / 1000)
    $remainder = $milliseconds % 1000
    return "{0:00}:{1:00}:{2:00}{3}{4:000}" -f $hours,$minutes,$wholeSeconds,$Separator,$remainder
}

$powerPoint = $null
$presentation = $null
try {
    $powerPoint = New-Object -ComObject PowerPoint.Application
    $powerPoint.Visible = -1
    $presentation = $powerPoint.Presentations.Add()
    $presentation.PageSetup.SlideWidth = 960
    $presentation.PageSetup.SlideHeight = 540

    $background = Get-Rgb 20 22 25
    $panel = Get-Rgb 35 39 43
    $white = Get-Rgb 248 248 246
    $muted = Get-Rgb 187 193 199
    $oracleRed = Get-Rgb 199 70 52

    for ($index = 0; $index -lt $scenes.Count; $index++) {
        $scene = $scenes[$index]
        $narrationPath = Join-Path (Join-Path $videoRoot "narration") $scene.Narration
        $narration = (Get-Content -LiteralPath $narrationPath -Raw).Trim()
        $slide = $presentation.Slides.Add($index + 1, 12)
        $slide.FollowMasterBackground = 0
        $slide.Background.Fill.ForeColor.RGB = $background

        Add-TextBox $slide $scene.Eyebrow 48 26 864 24 15 $oracleRed $true 1 | Out-Null
        Add-TextBox $slide $scene.Title 48 55 864 48 28 $white $true 1 | Out-Null

        if ($scene.Image) {
            $panelShape = $slide.Shapes.AddShape(5, 48, 116, 864, 304)
            $panelShape.Fill.ForeColor.RGB = $panel
            $panelShape.Line.Visible = 0
            $imagePath = if ([IO.Path]::IsPathRooted($scene.Image)) {
                $scene.Image
            }
            else {
                Join-Path $imagesRoot $scene.Image
            }
            Add-FittedPicture $slide $imagePath 60 128 840 280 | Out-Null
        }
        else {
            $panelShape = $slide.Shapes.AddShape(5, 92, 142, 776, 238)
            $panelShape.Fill.ForeColor.RGB = $panel
            $panelShape.Line.Visible = 0
            $summary = if ($index -eq 0) {
                "Gemini Enterprise`nA2A + native A2UI`n`nChatGPT and Claude`nMCP Apps + sandboxed HTML`n`nOracle AI Database + Java MCP Toolkit"
            }
            else {
                "Synthetic read-only validation`nOAuth 2.1 before customer data or writes`nActor-bound approval and durable idempotency`nOracle revalidation, locking, transaction, and audit`nRemove temporary anonymous access immediately"
            }
            Add-TextBox $slide $summary 122 166 716 190 23 $white $true 2 | Out-Null
        }

        $captionPanel = $slide.Shapes.AddShape(1, 32, 438, 896, 78)
        $captionPanel.Fill.ForeColor.RGB = Get-Rgb 8 9 10
        $captionPanel.Fill.Transparency = 0.08
        $captionPanel.Line.Visible = 0
        Add-TextBox $slide $narration 52 450 856 54 17 $white $false 2 | Out-Null
    }

    $presentation.SaveAs($deckPath)
    $presentation.Slides.Item(1).Export($posterPath, "PNG", 1920, 1080)

    $srt = New-Object System.Text.StringBuilder
    $vtt = New-Object System.Text.StringBuilder
    [void]$vtt.Append("WEBVTT`r`n`r`n")
    $cueNumber = 1
    for ($index = 0; $index -lt $scenes.Count; $index++) {
        $narrationPath = Join-Path (Join-Path $videoRoot "narration") $scenes[$index].Narration
        $narration = (Get-Content -LiteralPath $narrationPath -Raw).Trim()
        $sentences = @($narration -split '(?<=[.!?])\s+' | Where-Object { $_ })
        $cueDuration = $SecondsPerSlide / [Math]::Max(1, $sentences.Count)
        for ($sentenceIndex = 0; $sentenceIndex -lt $sentences.Count; $sentenceIndex++) {
            $start = ($index * $SecondsPerSlide) + ($sentenceIndex * $cueDuration)
            $end = ($index * $SecondsPerSlide) + (($sentenceIndex + 1) * $cueDuration) - 0.1
            [void]$srt.Append("$cueNumber`r`n$(Format-Time $start ',') --> $(Format-Time $end ',')`r`n$($sentences[$sentenceIndex])`r`n`r`n")
            [void]$vtt.Append("$(Format-Time $start '.') --> $(Format-Time $end '.')`r`n$($sentences[$sentenceIndex])`r`n`r`n")
            $cueNumber++
        }
    }
    [IO.File]::WriteAllText($srtPath, $srt.ToString(), [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText($vttPath, $vtt.ToString(), [Text.UTF8Encoding]::new($false))

    $presentation.CreateVideo($videoPath, 0, $SecondsPerSlide, 1080, 24, 85)
    $deadline = [DateTime]::UtcNow.AddMinutes(15)
    while ($presentation.CreateVideoStatus -in @(1, 2)) {
        if ([DateTime]::UtcNow -gt $deadline) {
            throw "PowerPoint video export exceeded 15 minutes."
        }
        Start-Sleep -Seconds 2
    }
    if ($presentation.CreateVideoStatus -ne 3 -or -not (Test-Path -LiteralPath $videoPath)) {
        throw "PowerPoint video export failed with status $($presentation.CreateVideoStatus)."
    }
}
finally {
    if ($presentation) { $presentation.Close() }
    if ($powerPoint) { $powerPoint.Quit() }
    if ($presentation) { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($presentation) }
    if ($powerPoint) { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($powerPoint) }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}

Write-Host "Created $videoPath"
Write-Host "Expected duration: $($scenes.Count * $SecondsPerSlide) seconds"
