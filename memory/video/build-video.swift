#!/usr/bin/env swift

import AppKit
import AVFoundation
import CoreVideo
import Foundation

let width = 1920
let height = 1080
let fps: Int32 = 24
let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let memoryRoot = root.deletingLastPathComponent()
let build = root.appendingPathComponent(".build")
let output = root.appendingPathComponent("memory-agent-walkthrough.mp4")
let poster = root.appendingPathComponent("memory-agent-walkthrough-poster.png")
let srt = root.appendingPathComponent("memory-agent-walkthrough.srt")
let vtt = root.appendingPathComponent("memory-agent-walkthrough.vtt")

enum Kind {
    case title
    case types
    case architecture
    case pythonCode
    case pythonApp
    case javaCode
    case javaApp
    case dream
    case recap
}

struct Scene {
    let kind: Kind
    let eyebrow: String
    let title: String
    let subtitle: String
    let caption: String
    let seconds: Double
}

let scenes = [
    Scene(kind: .title, eyebrow: "ORACLE AI DATABASE · AGENT MEMORY", title: "Memories Are\nthe Magic", subtitle: "Python · Java · governed continual learning", caption: "Build durable AI agent memory with Oracle AI Database, Java, and Python.", seconds: 8),
    Scene(kind: .types, eyebrow: "MEMORY ENGINEERING", title: "Keep each kind\nin its proper place", subtitle: "Working · episodic · semantic · procedural", caption: "Working state is temporary. Episodes, facts, and approved procedures have different lifecycles.", seconds: 9),
    Scene(kind: .architecture, eyebrow: "READ BEFORE · WRITE AFTER", title: "The agent is a loop\nwith external memory", subtitle: "Model reasoning plus a durable, governed substrate", caption: "Recall scoped context before a turn, then retain selected experience after the outcome.", seconds: 9),
    Scene(kind: .pythonCode, eyebrow: "PUBLIC PYTHON SDK", title: "Use Oracle AI\nAgent Memory 26.6", subtitle: "Managed objects · exact scopes · keyword search · TTL", caption: "The Python reference uses the public Oracle AI Agent Memory SDK and a real database pool.", seconds: 10),
    Scene(kind: .pythonApp, eyebrow: "VERIFIED PYTHON RUN", title: "Correct, expire,\napprove, and isolate", subtitle: "4 memories · 3 traces · 1 guideline · 0 leaks", caption: "The final SDK state contains the corrected fact, no expired route, and no Ava data in Leo's scope.", seconds: 10),
    Scene(kind: .javaCode, eyebrow: "JAVA 21 · JDBC · UCP", title: "Make the database\nmechanics visible", subtitle: "Transactions · versions · trace audit · approval", caption: "The Java reference exposes the SQL lifecycle and transaction boundaries for teaching and inspection.", seconds: 10),
    Scene(kind: .javaApp, eyebrow: "VERIFIED JAVA RUN", title: "Transfer the lesson,\nnot private memory", subtitle: "The same scenario, rendered from explicit database records", caption: "Leo reuses an approved shared procedure while Ava's private memory stays outside his scope.", seconds: 10),
    Scene(kind: .dream, eyebrow: "CONTINUAL LEARNING", title: "Learn first in\ntoken space", subtitle: "Episodes · induction · review · reusable skill", caption: "Successful traces can propose a structured skill, but a human approval gate controls activation.", seconds: 10),
    Scene(kind: .recap, eyebrow: "THE FOUR RS", title: "Retain · Recall\nReuse · Refine", subtitle: "Continuity without surrendering correction, lifecycle, or trust", caption: "Oracle AI Database keeps agent memory durable, scoped, correctable, expirable, and auditable.", seconds: 9)
]

let fm = FileManager.default
try fm.createDirectory(at: build, withIntermediateDirectories: true)
for file in [output, poster, srt, vtt] where fm.fileExists(atPath: file.path) {
    try fm.removeItem(at: file)
}

let red = NSColor(calibratedRed: 0.78, green: 0.18, blue: 0.12, alpha: 1)
let gold = NSColor(calibratedRed: 0.92, green: 0.66, blue: 0.28, alpha: 1)
let teal = NSColor(calibratedRed: 0.12, green: 0.51, blue: 0.49, alpha: 1)
let cream = NSColor(calibratedRed: 0.97, green: 0.95, blue: 0.90, alpha: 1)
let ink = NSColor(calibratedRed: 0.06, green: 0.12, blue: 0.14, alpha: 1)
let muted = NSColor(calibratedWhite: 0.68, alpha: 1)
let panel = NSColor(calibratedWhite: 0.13, alpha: 1)

func rounded(_ rect: NSRect, radius: CGFloat, color: NSColor) {
    color.setFill()
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
}

func drawText(_ value: String, _ rect: NSRect, font: NSFont, color: NSColor, alignment: NSTextAlignment = .left, spacing: CGFloat = 4) {
    let paragraph = NSMutableParagraphStyle()
    paragraph.alignment = alignment
    paragraph.lineSpacing = spacing
    paragraph.lineBreakMode = .byWordWrapping
    value.draw(with: rect, options: [.usesLineFragmentOrigin, .usesFontLeading], attributes: [
        .font: font,
        .foregroundColor: color,
        .paragraphStyle: paragraph
    ])
}

func badge(_ label: String, x: CGFloat, y: CGFloat, color: NSColor) {
    let size = (label as NSString).size(withAttributes: [.font: NSFont.boldSystemFont(ofSize: 22)])
    rounded(NSRect(x: x, y: y, width: size.width + 34, height: 44), radius: 22, color: color.withAlphaComponent(0.22))
    drawText(label, NSRect(x: x + 17, y: y + 8, width: size.width, height: 30), font: .boldSystemFont(ofSize: 22), color: color)
}

func sourceExcerpt(path: URL, marker: String, lines: Int) -> String {
    let text = (try? String(contentsOf: path, encoding: .utf8)) ?? "Source file unavailable"
    let sourceLines = text.components(separatedBy: .newlines)
    let start = sourceLines.firstIndex(where: { $0.contains(marker) }) ?? 0
    let lower = max(0, start - 2)
    let upper = min(sourceLines.count, lower + lines)
    return sourceLines[lower..<upper].enumerated().map {
        String(format: "%3d  %@", lower + $0.offset + 1, $0.element)
    }.joined(separator: "\n")
}

func drawCode(label: String, excerpt: String) {
    let rect = NSRect(x: 690, y: 165, width: 1135, height: 685)
    rounded(rect, radius: 22, color: NSColor(calibratedWhite: 0.095, alpha: 1))
    rounded(NSRect(x: rect.minX, y: rect.minY, width: rect.width, height: 64), radius: 22, color: NSColor(calibratedWhite: 0.16, alpha: 1))
    for (index, color) in [NSColor.systemRed, .systemYellow, .systemGreen].enumerated() {
        color.setFill()
        NSBezierPath(ovalIn: NSRect(x: rect.minX + 24 + CGFloat(index) * 31, y: rect.minY + 23, width: 16, height: 16)).fill()
    }
    drawText(label, NSRect(x: rect.minX + 125, y: rect.minY + 17, width: rect.width - 150, height: 36), font: .monospacedSystemFont(ofSize: 22, weight: .semibold), color: muted)
    drawText(excerpt, NSRect(x: rect.minX + 34, y: rect.minY + 88, width: rect.width - 68, height: rect.height - 112), font: .monospacedSystemFont(ofSize: 22, weight: .regular), color: NSColor(calibratedRed: 0.84, green: 0.89, blue: 0.91, alpha: 1), spacing: 8)
}

func drawImage(path: URL) {
    guard let image = NSImage(contentsOf: path) else {
        drawText("Verified application capture unavailable", NSRect(x: 690, y: 360, width: 1100, height: 80), font: .boldSystemFont(ofSize: 34), color: .white, alignment: .center)
        return
    }
    let frame = NSRect(x: 665, y: 155, width: 1180, height: 700)
    rounded(frame, radius: 22, color: panel)
    let scale = min((frame.width - 20) / image.size.width, (frame.height - 20) / image.size.height)
    let size = NSSize(width: image.size.width * scale, height: image.size.height * scale)
    let target = NSRect(x: frame.midX - size.width / 2, y: frame.midY - size.height / 2, width: size.width, height: size.height)
    image.draw(
        in: target,
        from: NSRect(origin: .zero, size: image.size),
        operation: .sourceOver,
        fraction: 1,
        respectFlipped: true,
        hints: [.interpolation: NSImageInterpolation.high]
    )
}

func drawTypes() {
    let items = [
        ("01", "WORKING", "Current task\nand active plan"),
        ("02", "EPISODIC", "What happened\nand its outcome"),
        ("03", "SEMANTIC", "Durable facts\nand preferences"),
        ("04", "PROCEDURAL", "Approved reusable\nways of working")
    ]
    for (index, item) in items.enumerated() {
        let x = 650 + CGFloat(index % 2) * 580
        let y = 205 + CGFloat(index / 2) * 260
        rounded(NSRect(x: x, y: y, width: 530, height: 210), radius: 22, color: panel)
        drawText(item.0, NSRect(x: x + 28, y: y + 25, width: 70, height: 35), font: .boldSystemFont(ofSize: 24), color: red)
        drawText(item.1, NSRect(x: x + 105, y: y + 27, width: 370, height: 35), font: .boldSystemFont(ofSize: 25), color: .white)
        drawText(item.2, NSRect(x: x + 34, y: y + 86, width: 450, height: 90), font: .systemFont(ofSize: 28), color: muted, alignment: .center)
    }
}

func drawArchitecture() {
    rounded(NSRect(x: 805, y: 180, width: 460, height: 95), radius: 47, color: gold)
    drawText("AGENT LOOP", NSRect(x: 835, y: 208, width: 400, height: 38), font: .boldSystemFont(ofSize: 30), color: ink, alignment: .center)
    drawText("READ BEFORE", NSRect(x: 645, y: 340, width: 300, height: 35), font: .boldSystemFont(ofSize: 22), color: teal, alignment: .center)
    drawText("WRITE AFTER", NSRect(x: 1125, y: 340, width: 300, height: 35), font: .boldSystemFont(ofSize: 22), color: red, alignment: .center)
    drawText("↓", NSRect(x: 985, y: 300, width: 100, height: 80), font: .boldSystemFont(ofSize: 62), color: .white, alignment: .center)
    rounded(NSRect(x: 680, y: 410, width: 710, height: 300), radius: 28, color: panel)
    drawText("ORACLE AI DATABASE", NSRect(x: 735, y: 452, width: 600, height: 50), font: .boldSystemFont(ofSize: 38), color: .white, alignment: .center)
    drawText("identity scope · search · versions · TTL\ntraces · approval · audit · transactions", NSRect(x: 735, y: 530, width: 600, height: 110), font: .systemFont(ofSize: 28), color: muted, alignment: .center, spacing: 12)
}

func drawDream() {
    let labels = ["EPISODES", "INDUCTION", "PENDING SKILL", "HUMAN REVIEW", "APPROVED REUSE"]
    for (index, label) in labels.enumerated() {
        let x = 560 + CGFloat(index) * 265
        rounded(NSRect(x: x, y: 400, width: 220, height: 110), radius: 20, color: index == 4 ? teal : panel)
        drawText(label, NSRect(x: x + 18, y: 434, width: 184, height: 50), font: .boldSystemFont(ofSize: 20), color: .white, alignment: .center)
        if index < labels.count - 1 {
            drawText("→", NSRect(x: x + 220, y: 428, width: 45, height: 50), font: .boldSystemFont(ofSize: 32), color: gold, alignment: .center)
        }
    }
    drawText("NO MODEL-WEIGHT UPDATE REQUIRED", NSRect(x: 650, y: 585, width: 1130, height: 55), font: .boldSystemFont(ofSize: 28), color: gold, alignment: .center)
}

func drawRecap() {
    for (index, label) in ["RETAIN", "RECALL", "REUSE", "REFINE"].enumerated() {
        let x = 610 + CGFloat(index) * 300
        rounded(NSRect(x: x, y: 360, width: 250, height: 150), radius: 75, color: index % 2 == 0 ? red : teal)
        drawText(label, NSRect(x: x + 25, y: 412, width: 200, height: 45), font: .boldSystemFont(ofSize: 27), color: .white, alignment: .center)
    }
    drawText("DURABLE · SCOPED · CORRECTABLE · EXPIRABLE · AUDITABLE", NSRect(x: 560, y: 585, width: 1280, height: 60), font: .boldSystemFont(ofSize: 28), color: gold, alignment: .center)
}

func imageFor(index: Int) -> NSImage {
    let scene = scenes[index]
    let image = NSImage(size: NSSize(width: width, height: height))
    image.lockFocusFlipped(true)
    ink.setFill()
    NSBezierPath(rect: NSRect(x: 0, y: 0, width: width, height: height)).fill()
    rounded(NSRect(x: 0, y: 0, width: 18, height: height), radius: 0, color: red)
    drawText(scene.eyebrow, NSRect(x: 85, y: 62, width: 1100, height: 34), font: .boldSystemFont(ofSize: 22), color: red)
    drawText(scene.title, NSRect(x: 85, y: 115, width: 540, height: 190), font: .boldSystemFont(ofSize: scene.kind == .title ? 76 : 58), color: .white, spacing: 3)
    drawText(scene.subtitle, NSRect(x: 90, y: 315, width: 525, height: 110), font: .systemFont(ofSize: 27), color: muted, spacing: 7)

    switch scene.kind {
    case .title:
        badge("ORACLE AI AGENT MEMORY", x: 90, y: 515, color: red)
        badge("PYTHON", x: 90, y: 580, color: gold)
        badge("JAVA 21 + UCP", x: 235, y: 580, color: teal)
        rounded(NSRect(x: 760, y: 230, width: 900, height: 420), radius: 34, color: panel)
        drawText("MODEL\nREASONING", NSRect(x: 835, y: 335, width: 260, height: 120), font: .boldSystemFont(ofSize: 34), color: .white, alignment: .center)
        drawText("↔", NSRect(x: 1120, y: 360, width: 150, height: 80), font: .boldSystemFont(ofSize: 60), color: gold, alignment: .center)
        drawText("GOVERNED\nMEMORY", NSRect(x: 1280, y: 335, width: 300, height: 120), font: .boldSystemFont(ofSize: 34), color: .white, alignment: .center)
    case .types:
        drawTypes()
    case .architecture:
        drawArchitecture()
    case .pythonCode:
        let path = memoryRoot.appendingPathComponent("python-app/app.py")
        drawCode(label: "memory/python-app/app.py", excerpt: sourceExcerpt(path: path, marker: "self.memory = OracleAgentMemory", lines: 18))
    case .pythonApp:
        drawImage(path: memoryRoot.appendingPathComponent("images/python-oracle-agent-memory-demo.png"))
    case .javaCode:
        let path = memoryRoot.appendingPathComponent("app/server/src/main/java/com/oracle/demo/memory/MemoryRepository.java")
        drawCode(label: "memory/app/server/.../MemoryRepository.java", excerpt: sourceExcerpt(path: path, marker: "Map<String, Object> correct()", lines: 19))
    case .javaApp:
        drawImage(path: memoryRoot.appendingPathComponent("images/java-oracle-ai-database-memory-demo.png"))
    case .dream:
        drawDream()
    case .recap:
        drawRecap()
    }

    rounded(NSRect(x: 150, y: 900, width: 1620, height: 120), radius: 20, color: NSColor(calibratedWhite: 0.02, alpha: 0.94))
    drawText(scene.caption, NSRect(x: 190, y: 930, width: 1540, height: 70), font: .boldSystemFont(ofSize: 29), color: .white, alignment: .center)
    drawText(String(format: "%02d / %02d", index + 1, scenes.count), NSRect(x: 1690, y: 66, width: 150, height: 28), font: .monospacedSystemFont(ofSize: 18, weight: .medium), color: muted, alignment: .right)
    image.unlockFocus()
    return image
}

func cgImage(_ image: NSImage) -> CGImage {
    var rect = NSRect(origin: .zero, size: image.size)
    return image.cgImage(forProposedRect: &rect, context: nil, hints: nil)!
}

func timestamp(_ seconds: Double, separator: String) -> String {
    let milliseconds = Int((seconds * 1000).rounded())
    return String(
        format: "%02d:%02d:%02d%@%03d",
        milliseconds / 3_600_000,
        (milliseconds / 60_000) % 60,
        (milliseconds / 1000) % 60,
        separator,
        milliseconds % 1000
    )
}

var cursor = 0.0
var srtText = ""
var vttText = "WEBVTT\n\n"
for (index, scene) in scenes.enumerated() {
    let end = cursor + scene.seconds
    srtText += "\(index + 1)\n\(timestamp(cursor, separator: ",")) --> \(timestamp(end, separator: ","))\n\(scene.caption)\n\n"
    vttText += "\(timestamp(cursor, separator: ".")) --> \(timestamp(end, separator: "."))\n\(scene.caption)\n\n"
    cursor = end
}
try (srtText.trimmingCharacters(in: .newlines) + "\n").write(
    to: srt,
    atomically: true,
    encoding: .utf8
)
try (vttText.trimmingCharacters(in: .newlines) + "\n").write(
    to: vtt,
    atomically: true,
    encoding: .utf8
)

let posterImage = imageFor(index: 0)
let posterRep = NSBitmapImageRep(cgImage: cgImage(posterImage))
try posterRep.representation(using: .png, properties: [:])!.write(to: poster)

let writer = try AVAssetWriter(outputURL: output, fileType: .mp4)
let settings: [String: Any] = [
    AVVideoCodecKey: AVVideoCodecType.h264,
    AVVideoWidthKey: width,
    AVVideoHeightKey: height,
    AVVideoCompressionPropertiesKey: [
        AVVideoAverageBitRateKey: 7_000_000,
        AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel
    ]
]
let input = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
let attributes: [String: Any] = [
    kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB,
    kCVPixelBufferWidthKey as String: width,
    kCVPixelBufferHeightKey as String: height
]
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: attributes)
guard writer.canAdd(input) else { fatalError("Cannot add video input") }
writer.add(input)
writer.startWriting()
writer.startSession(atSourceTime: .zero)

let colorSpace = CGColorSpaceCreateDeviceRGB()
let rendered = scenes.indices.map { cgImage(imageFor(index: $0)) }
let totalFrames = Int(ceil(cursor * Double(fps)))
var sceneIndex = 0
var sceneEnd = scenes[0].seconds
for frame in 0..<totalFrames {
    let seconds = Double(frame) / Double(fps)
    while seconds >= sceneEnd && sceneIndex < scenes.count - 1 {
        sceneIndex += 1
        sceneEnd += scenes[sceneIndex].seconds
    }
    while !input.isReadyForMoreMediaData { Thread.sleep(forTimeInterval: 0.002) }
    var buffer: CVPixelBuffer?
    CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32ARGB, attributes as CFDictionary, &buffer)
    guard let pixelBuffer = buffer else { fatalError("Cannot allocate pixel buffer") }
    CVPixelBufferLockBaseAddress(pixelBuffer, [])
    let context = CGContext(
        data: CVPixelBufferGetBaseAddress(pixelBuffer),
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: CVPixelBufferGetBytesPerRow(pixelBuffer),
        space: colorSpace,
        bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
    )!
    context.draw(rendered[sceneIndex], in: CGRect(x: 0, y: 0, width: width, height: height))
    CVPixelBufferUnlockBaseAddress(pixelBuffer, [])
    adaptor.append(pixelBuffer, withPresentationTime: CMTime(value: Int64(frame), timescale: fps))
}
input.markAsFinished()
let semaphore = DispatchSemaphore(value: 0)
writer.finishWriting { semaphore.signal() }
semaphore.wait()
guard writer.status == .completed else { throw writer.error ?? NSError(domain: "MemoryVideo", code: 1) }

print(String(format: "Created %@ (%.1f seconds)", output.lastPathComponent, cursor))
print("Created poster, SRT, and WebVTT caption assets")
