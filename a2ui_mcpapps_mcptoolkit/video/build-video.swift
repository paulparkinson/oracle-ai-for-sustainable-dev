#!/usr/bin/env swift

import AppKit
import AVFoundation
import CoreVideo
import Foundation

let width = 1920
let height = 1080
let fps: Int32 = 24
let secondsPerScene = 12.0
let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let imageRoot = root.deletingLastPathComponent().appendingPathComponent("images")
let output = root.appendingPathComponent("interactive-ai-walkthrough.mp4")
let poster = root.appendingPathComponent("interactive-ai-walkthrough-poster.png")
let srtURL = root.appendingPathComponent("interactive-ai-walkthrough.srt")
let vttURL = root.appendingPathComponent("interactive-ai-walkthrough.vtt")
try FileManager.default.createDirectory(at: root.appendingPathComponent(".build"), withIntermediateDirectories: true)

struct Scene {
    let eyebrow: String
    let title: String
    let caption: String
    let detail: String
    let image: String?
    let file: String?
    let code: String?
    let cursorFrom: CGPoint
    let cursorTo: CGPoint
}

let scenes = [
    Scene(eyebrow: "A2UI + MCP APPS", title: "Two UI contracts.\nOne governed workflow.", caption: "Compare native A2UI with portable MCP Apps over the same Oracle-governed supply-chain service.", detail: "Silent walkthrough · sanitized captures · animated interactions", image: nil, file: nil, code: nil, cursorFrom: CGPoint(x: 390, y: 650), cursorTo: CGPoint(x: 770, y: 650)),
    Scene(eyebrow: "RESPONSIBILITY MAP", title: "Keep transport, UI,\nand tools distinct.", caption: "A2A connects hosts to agents. A2UI declares native UI. MCP Apps return sandboxed web UI. MCP exposes governed tools.", detail: "A2A → agent   A2UI → native components   MCP Apps → sandboxed web app   MCP → tools", image: nil, file: nil, code: nil, cursorFrom: CGPoint(x: 840, y: 520), cursorTo: CGPoint(x: 1450, y: 700)),
    Scene(eyebrow: "CAPABILITY ADVERTISEMENT", title: "The A2A card declares\nA2UI support.", caption: "The host discovers A2UI v0.8 and its supported component catalog before it renders any response.", detail: "Protocol and UI extension versions are separate declarations.", image: nil, file: "gemini-enterprise-a2a/main.py", code: """
capabilities = AgentCapabilities(
  streaming=True,
  extensions=[AgentExtension(
    uri="https://a2ui.org/a2a-extension/a2ui/v0.8",
    params={"supportedCatalogIds": [STANDARD_CATALOG]}
  )]
)
protocol_version = "0.3.0"
""", cursorFrom: CGPoint(x: 1070, y: 485), cursorTo: CGPoint(x: 1370, y: 650)),
    Scene(eyebrow: "NATIVE A2UI", title: "Prompt the registered\nA2A agent.", caption: "The user asks for three recommendations above risk 70. The adapter calls the governed service and returns A2UI DataParts.", detail: "The host renders trusted native components, not agent-supplied JavaScript.", image: "gemini-enterprise-a2ui-review.png", file: nil, code: nil, cursorFrom: CGPoint(x: 1450, y: 290), cursorTo: CGPoint(x: 1410, y: 650)),
    Scene(eyebrow: "A2UI USER ACTION", title: "Review one exact\nOracle result.", caption: "Move to the recommendation, enter notes, and select the exact-transfer action. The host sends a bounded userAction back over A2A.", detail: "Approval ID + recommendation ID + notes · no client-selected quantity", image: "gemini-enterprise-a2ui-review.png", file: nil, code: nil, cursorFrom: CGPoint(x: 1410, y: 570), cursorTo: CGPoint(x: 940, y: 720)),
    Scene(eyebrow: "MCP APP CONTRACT", title: "Return a ui:// resource\nwith the tool result.", caption: "The MCP server exposes one bounded dashboard tool and points the host to a portable HTML application.", detail: "The host bridge mediates every tool call from the sandbox.", image: nil, file: "mcp-app/server.ts", code: """
registerAppTool(server,
  "show-inventory-transfer-dashboard", {
    inputSchema: {
      minimumStockoutRisk: z.number(),
      maximumRows: z.number().int()
    },
    _meta: { ui: { resourceUri:
      "ui://inventory-exchange/dashboard-v2" } }
  }, loadGovernedReview);
""", cursorFrom: CGPoint(x: 1110, y: 500), cursorTo: CGPoint(x: 1470, y: 690)),
    Scene(eyebrow: "MCP APP IN CHATGPT", title: "Call the tool, then\nload the dashboard.", caption: "ChatGPT displays the Toolkit result and loads the sandboxed MCP App. The cursor selects Review this transfer inside the supplied interface.", detail: "The iframe has no wallet, database password, or unrestricted SQL capability.", image: "chatgpt-mcp-app-dashboard.png", file: nil, code: nil, cursorFrom: CGPoint(x: 1520, y: 420), cursorTo: CGPoint(x: 950, y: 720)),
    Scene(eyebrow: "MCP APP IN CLAUDE", title: "Reuse the same\nportable application.", caption: "Claude discovers the same bounded tool and renders the same ui:// dashboard through its connector and sandbox bridge.", detail: "Host-specific connection · shared MCP resource · shared Oracle transaction boundary", image: "claude-mcp-app-dashboard.png", file: nil, code: nil, cursorFrom: CGPoint(x: 1460, y: 370), cursorTo: CGPoint(x: 960, y: 720)),
    Scene(eyebrow: "MCP APP IN GEMINI ENTERPRISE", title: "Connect a private\nCustom MCP Server.", caption: "Create an OAuth-backed private connector, reload custom actions, and enable only show-inventory-transfer-dashboard.", detail: "PKCE + HTTP Basic · private Cloud Run · Discovery Engine invoker only", image: nil, file: "docs/gemini-enterprise-mcp-app.md", code: """
Authorization URL:
  https://accounts.google.com/o/oauth2/auth
Parameters: &access_type=offline
Token URL:
  https://oauth2.googleapis.com/token
Scopes: openid email profile cloud-platform
PKCE: enabled
HTTP Basic authentication: enabled
""", cursorFrom: CGPoint(x: 1080, y: 480), cursorTo: CGPoint(x: 1450, y: 730)),
    Scene(eyebrow: "DATABASE BOUNDARY", title: "Every UI path reaches\none safe operation.", caption: "The service binds approval to the actor and exact recommendation. Oracle locks, revalidates, audits, and commits atomically.", detail: "A2UI and MCP Apps change presentation, not database authority.", image: nil, file: "database/06-mcp-procedure.sql", code: """
SELECT ... FOR UPDATE;
v_safe_qty := LEAST(source_surplus, target_shortage);
IF p_transfer_qty > v_safe_qty THEN
  RAISE_APPLICATION_ERROR(-20007, 'Stale');
END IF;
INSERT INTO inventory_transfers (...);
UPDATE inventory_positions SET reserved_qty = ...;
COMMIT;
""", cursorFrom: CGPoint(x: 1040, y: 490), cursorTo: CGPoint(x: 1420, y: 770)),
    Scene(eyebrow: "SECURITY CHECK", title: "Keep identity outside\nthe prompt.", caption: "Deep Data Security filters rows by the authenticated database identity before the Toolkit, service, host, or model receives them.", detail: "No secrets in captures · no identity chosen from prompt text · no unrestricted tool", image: nil, file: "database/07-deep-data-security.sql", code: """
CREATE DATA GRANT inventory_environmental_read
  AS SELECT ON stockout_transfer_recommendation_v
  WHERE category_name = 'Environmental Monitoring'
  TO inventory_environmental_role;

SET USE DATA GRANTS ONLY
  ON stockout_transfer_recommendation_v ENABLED;
""", cursorFrom: CGPoint(x: 1020, y: 510), cursorTo: CGPoint(x: 1440, y: 700)),
    Scene(eyebrow: "RESULT", title: "Choose the UI contract\nthat fits the host.", caption: "Use A2UI for native, catalog-controlled components. Use MCP Apps for rich portable web interfaces. Keep Oracle AI Database authoritative.", detail: "A2UI · consistent and safe   MCP Apps · flexible and portable", image: nil, file: nil, code: nil, cursorFrom: CGPoint(x: 620, y: 650), cursorTo: CGPoint(x: 1310, y: 650))
]

let bg = NSColor(calibratedRed: 0.035, green: 0.055, blue: 0.075, alpha: 1)
let panel = NSColor(calibratedRed: 0.075, green: 0.105, blue: 0.13, alpha: 1)
let cyan = NSColor(calibratedRed: 0.12, green: 0.82, blue: 0.92, alpha: 1)
let green = NSColor(calibratedRed: 0.30, green: 0.86, blue: 0.55, alpha: 1)
let orange = NSColor(calibratedRed: 0.96, green: 0.48, blue: 0.23, alpha: 1)
let muted = NSColor(calibratedWhite: 0.72, alpha: 1)

func rounded(_ rect: NSRect, _ radius: CGFloat, _ color: NSColor) {
    color.setFill(); NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
}

func drawText(_ value: String, _ rect: NSRect, _ font: NSFont, _ color: NSColor, _ alignment: NSTextAlignment = .left) {
    let p = NSMutableParagraphStyle(); p.alignment = alignment; p.lineSpacing = 5; p.lineBreakMode = .byWordWrapping
    value.draw(with: rect, options: [.usesLineFragmentOrigin, .usesFontLeading], attributes: [.font: font, .foregroundColor: color, .paragraphStyle: p])
}

func drawBadge(_ value: String, x: CGFloat, y: CGFloat, color: NSColor) {
    let size = (value as NSString).size(withAttributes: [.font: NSFont.boldSystemFont(ofSize: 21)])
    rounded(NSRect(x: x, y: y, width: size.width + 34, height: 42), 21, color.withAlphaComponent(0.20))
    drawText(value, NSRect(x: x + 17, y: y + 8, width: size.width, height: 26), .boldSystemFont(ofSize: 21), color)
}

func drawScreenshot(_ name: String, in rect: NSRect) {
    guard let image = NSImage(contentsOf: imageRoot.appendingPathComponent(name)) else { return }
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: rect, xRadius: 20, yRadius: 20).addClip()
    let source = NSRect(x: 0, y: 0, width: image.size.width, height: image.size.height * 0.94)
    image.draw(in: rect, from: source, operation: .sourceOver, fraction: 1, respectFlipped: true, hints: [.interpolation: NSImageInterpolation.high])
    NSGraphicsContext.restoreGraphicsState()
    NSColor(calibratedWhite: 0.4, alpha: 0.7).setStroke(); NSBezierPath(roundedRect: rect, xRadius: 20, yRadius: 20).stroke()
}

func drawCode(_ file: String, _ code: String, in rect: NSRect) {
    rounded(rect, 20, NSColor(calibratedWhite: 0.055, alpha: 1))
    rounded(NSRect(x: rect.minX, y: rect.minY, width: rect.width, height: 58), 20, panel)
    drawText(file, NSRect(x: rect.minX + 30, y: rect.minY + 15, width: rect.width - 60, height: 30), .monospacedSystemFont(ofSize: 21, weight: .semibold), muted)
    let numbered = code.split(separator: "\n", omittingEmptySubsequences: false).enumerated().map { String(format: "%2d  %@", $0.offset + 1, String($0.element)) }.joined(separator: "\n")
    drawText(numbered, NSRect(x: rect.minX + 28, y: rect.minY + 82, width: rect.width - 56, height: rect.height - 105), .monospacedSystemFont(ofSize: 24, weight: .regular), NSColor(calibratedRed: 0.82, green: 0.89, blue: 0.93, alpha: 1))
}

func baseImage(_ scene: Scene, index: Int) -> NSImage {
    let image = NSImage(size: NSSize(width: width, height: height)); image.lockFocusFlipped(true)
    bg.setFill(); NSBezierPath(rect: NSRect(x: 0, y: 0, width: width, height: height)).fill()
    cyan.withAlphaComponent(0.08).setStroke()
    for x in stride(from: 0, through: width, by: 80) { let p = NSBezierPath(); p.move(to: NSPoint(x: x, y: 0)); p.line(to: NSPoint(x: x, y: height)); p.stroke() }
    for y in stride(from: 0, through: height, by: 80) { let p = NSBezierPath(); p.move(to: NSPoint(x: 0, y: y)); p.line(to: NSPoint(x: width, y: y)); p.stroke() }
    drawText(scene.eyebrow, NSRect(x: 70, y: 58, width: 800, height: 32), .boldSystemFont(ofSize: 22), cyan)
    drawText(scene.title, NSRect(x: 70, y: 112, width: 580, height: 190), .boldSystemFont(ofSize: index == 0 ? 68 : 52), .white)
    drawText(scene.detail, NSRect(x: 74, y: 325, width: 555, height: 105), .systemFont(ofSize: 24), muted)
    let visual = NSRect(x: 690, y: 120, width: 1160, height: 700)
    if let name = scene.image { drawScreenshot(name, in: visual) }
    else if let file = scene.file, let code = scene.code { drawCode(file, code, in: visual) }
    else {
        rounded(visual, 24, panel)
        drawBadge("A2A", x: 790, y: 360, color: cyan)
        drawBadge("A2UI", x: 990, y: 360, color: green)
        drawBadge("MCP Apps", x: 1210, y: 360, color: orange)
        drawBadge("Oracle AI Database", x: 1490, y: 360, color: cyan)
        drawText("discover  →  render  →  interact  →  govern", NSRect(x: 790, y: 485, width: 930, height: 55), .boldSystemFont(ofSize: 33), .white, .center)
    }
    rounded(NSRect(x: 135, y: 875, width: 1650, height: 135), 20, NSColor(calibratedWhite: 0.015, alpha: 0.94))
    drawText(scene.caption, NSRect(x: 180, y: 905, width: 1560, height: 78), .boldSystemFont(ofSize: 30), .white, .center)
    drawText(String(format: "%02d / %02d", index + 1, scenes.count), NSRect(x: 1690, y: 58, width: 160, height: 30), .monospacedSystemFont(ofSize: 18, weight: .medium), muted, .right)
    image.unlockFocus(); return image
}

func cgImage(_ image: NSImage) -> CGImage { var r = NSRect(origin: .zero, size: image.size); return image.cgImage(forProposedRect: &r, context: nil, hints: nil)! }

func stamp(_ seconds: Double, comma: Bool) -> String {
    let ms = Int((seconds * 1000).rounded()); return String(format: "%02d:%02d:%02d%@%03d", ms / 3_600_000, (ms / 60_000) % 60, (ms / 1000) % 60, comma ? "," : ".", ms % 1000)
}

var srt = ""; var vtt = "WEBVTT\n\n"
for (i, scene) in scenes.enumerated() {
    let start = Double(i) * secondsPerScene + 0.2; let end = Double(i + 1) * secondsPerScene - 0.2
    srt += "\(i + 1)\n\(stamp(start, comma: true)) --> \(stamp(end, comma: true))\n\(scene.caption)\n\n"
    vtt += "\(stamp(start, comma: false)) --> \(stamp(end, comma: false))\n\(scene.caption)\n\n"
}
try srt.write(to: srtURL, atomically: true, encoding: .utf8); try vtt.write(to: vttURL, atomically: true, encoding: .utf8)

let bases = scenes.enumerated().map { cgImage(baseImage($0.element, index: $0.offset)) }
let first = NSBitmapImageRep(cgImage: bases[0]); try first.representation(using: .png, properties: [:])!.write(to: poster)
if FileManager.default.fileExists(atPath: output.path) { try FileManager.default.removeItem(at: output) }
let writer = try AVAssetWriter(outputURL: output, fileType: .mp4)
let settings: [String: Any] = [AVVideoCodecKey: AVVideoCodecType.h264, AVVideoWidthKey: width, AVVideoHeightKey: height, AVVideoCompressionPropertiesKey: [AVVideoAverageBitRateKey: 7_000_000]]
let input = AVAssetWriterInput(mediaType: .video, outputSettings: settings); input.expectsMediaDataInRealTime = false
let attrs: [String: Any] = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB, kCVPixelBufferWidthKey as String: width, kCVPixelBufferHeightKey as String: height]
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: attrs); writer.add(input); writer.startWriting(); writer.startSession(atSourceTime: .zero)
let colorSpace = CGColorSpaceCreateDeviceRGB(); let totalFrames = Int(Double(scenes.count) * secondsPerScene * Double(fps))
for frame in 0..<totalFrames {
    while !input.isReadyForMoreMediaData { Thread.sleep(forTimeInterval: 0.001) }
    let t = Double(frame) / Double(fps); let sceneIndex = min(scenes.count - 1, Int(t / secondsPerScene)); let p = (t.truncatingRemainder(dividingBy: secondsPerScene)) / secondsPerScene
    var buffer: CVPixelBuffer?; CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32ARGB, attrs as CFDictionary, &buffer)
    let pixel = buffer!; CVPixelBufferLockBaseAddress(pixel, [])
    let c = CGContext(data: CVPixelBufferGetBaseAddress(pixel), width: width, height: height, bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(pixel), space: colorSpace, bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue)!
    c.draw(bases[sceneIndex], in: CGRect(x: 0, y: 0, width: width, height: height))
    let a = scenes[sceneIndex].cursorFrom, b = scenes[sceneIndex].cursorTo; let q = min(1, max(0, (p - 0.15) / 0.65)); let x = a.x + (b.x - a.x) * q; let yTop = a.y + (b.y - a.y) * q; let y = CGFloat(height) - yTop
    c.setFillColor(NSColor.white.cgColor); c.setStrokeColor(NSColor.black.withAlphaComponent(0.75).cgColor); c.setLineWidth(4)
    let cursor = CGMutablePath(); cursor.move(to: CGPoint(x: x, y: y)); cursor.addLine(to: CGPoint(x: x + 13, y: y - 42)); cursor.addLine(to: CGPoint(x: x + 24, y: y - 25)); cursor.addLine(to: CGPoint(x: x + 42, y: y - 34)); cursor.addLine(to: CGPoint(x: x + 49, y: y - 22)); cursor.addLine(to: CGPoint(x: x + 31, y: y - 13)); cursor.closeSubpath(); c.addPath(cursor); c.drawPath(using: .fillStroke)
    if p > 0.78 && p < 0.94 { let radius = CGFloat(22 + 55 * ((p - 0.78) / 0.16)); c.setStrokeColor(cyan.withAlphaComponent(0.9).cgColor); c.setLineWidth(6); c.strokeEllipse(in: CGRect(x: x - radius, y: y - radius, width: radius * 2, height: radius * 2)) }
    CVPixelBufferUnlockBaseAddress(pixel, []); adaptor.append(pixel, withPresentationTime: CMTime(value: Int64(frame), timescale: fps))
}
input.markAsFinished(); let sem = DispatchSemaphore(value: 0); writer.finishWriting { sem.signal() }; sem.wait()
guard writer.status == .completed else { throw writer.error ?? NSError(domain: "VideoBuild", code: 1) }
print("Created \(output.lastPathComponent), poster, SRT, and VTT (\(Int(Double(scenes.count) * secondsPerScene)) seconds).")
