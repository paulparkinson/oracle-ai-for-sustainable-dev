[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$videoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $videoRoot
$blogPath = Join-Path $projectRoot "blog.html"
$videoPath = Join-Path $videoRoot "interactive-ai-walkthrough.mp4"
$posterPath = Join-Path $videoRoot "interactive-ai-walkthrough-poster.png"
$srtPath = Join-Path $videoRoot "interactive-ai-walkthrough.srt"
$vttPath = Join-Path $videoRoot "interactive-ai-walkthrough.vtt"

foreach ($path in @($videoPath, $posterPath, $srtPath, $vttPath, $blogPath)) {
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing required asset: $path" }
}
if ((Get-Item -LiteralPath $videoPath).Length -lt 1MB) { throw "MP4 is unexpectedly small." }

$shell = New-Object -ComObject Shell.Application
$folder = $shell.NameSpace((Split-Path -Parent $videoPath))
$item = $folder.ParseName((Split-Path -Leaf $videoPath))
$metadata = @{}
for ($index = 0; $index -lt 320; $index++) {
    $name = $folder.GetDetailsOf($null, $index)
    if ($name) { $metadata[$name] = $folder.GetDetailsOf($item, $index) }
}
[void][Runtime.InteropServices.Marshal]::ReleaseComObject($shell)

$blog = Get-Content -LiteralPath $blogPath -Raw
foreach ($required in @('interactive-ai-walkthrough.mp4','interactive-ai-walkthrough-poster.png','interactive-ai-walkthrough.vtt','<video controls')) {
    if (-not $blog.Contains($required)) { throw "Blog is missing video reference: $required" }
}
$captions = Get-Content -LiteralPath $srtPath -Raw
foreach ($required in @('Gemini Enterprise','ChatGPT','Claude','Oracle AI Database','MCP Apps','A2UI')) {
    if (-not $captions.Contains($required)) { throw "Captions are missing: $required" }
}
if ($captions -match '(?i)(DB_PASSWORD|api[_-]?key|BEGIN PRIVATE KEY|Wallet_[A-Za-z0-9_-]+)') {
    throw "Captions contain a possible secret or wallet identifier."
}

Write-Host "Video bytes: $((Get-Item -LiteralPath $videoPath).Length)"
foreach ($key in @('Length','Frame width','Frame height','Video compression')) {
    if ($metadata.ContainsKey($key)) { Write-Host "$key`: $($metadata[$key])" }
}
Write-Host "Verified blog embed, captions, poster, and obvious-secret scan."
