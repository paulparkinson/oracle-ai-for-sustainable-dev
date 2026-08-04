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
let captureRoot = build.appendingPathComponent("captures")
let output = root.appendingPathComponent("memory-agent-walkthrough.mp4")
let poster = root.appendingPathComponent("memory-agent-walkthrough-poster.png")
let srt = root.appendingPathComponent("memory-agent-walkthrough.srt")
let vtt = root.appendingPathComponent("memory-agent-walkthrough.vtt")

enum Visual {
    case title
    case recordTypes
    case image(String)
    case code(String, String, Int)
    case recap
}

struct Scene {
    let visual: Visual
    let eyebrow: String
    let title: String
    let subtitle: String
    let caption: String
    let seconds: Double
}

let repositoryPath = "java-agent/server/src/main/java/com/oracle/demo/memory/MemoryRepository.java"
let libraryPath = "java-agent/server/src/main/java/com/oracle/demo/memory/AgentMemoryLibraryDemo.java"
let parkRepositoryPath = "java-agent/server/src/main/java/com/oracle/demo/memory/ParkExperienceRepository.java"

let scenes: [Scene] = [
    Scene(
        visual: .title,
        eyebrow: "ORACLE AI DATABASE + JAVA",
        title: "Memories Are\nthe Magic",
        subtitle: "A verified agent-memory walkthrough",
        caption: "Run every scenario action, inspect the Java implementation, and verify each change inside Oracle AI Database.",
        seconds: 8),
    Scene(
        visual: .recordTypes,
        eyebrow: "JAVA LIBRARY TAXONOMY",
        title: "RECORD_TYPE\nis not memory type",
        subtitle: "Stored classification and conceptual use are related, but different.",
        caption: "The library stores preference, fact, guideline, and memory records. Message represents recent conversation from a separate table.",
        seconds: 10),

    Scene(
        visual: .image("step-0-app.png"),
        eyebrow: "STEP 0 | SCENARIO EVENT",
        title: "Start from a\nprovable cold state",
        subtitle: "Both paths are reset.",
        caption: "The library panels become empty. The lifecycle path retains only the Ava and Leo visitor identities.",
        seconds: 7),
    Scene(
        visual: .code(libraryPath, "synchronized Map<String, Object> reset()", 16),
        eyebrow: "STEP 0 | JAVA LIBRARY ACTION",
        title: "Clear library\nmemory",
        subtitle: "OracleAgentMemory.clearAll() resets the live library path.",
        caption: "The Java library clears persisted messages and extracted records, then clears the in-process search result and context-card view.",
        seconds: 7),
    Scene(
        visual: .image("step-0-library-database.png"),
        eyebrow: "STEP 0 | ORACLE DATABASE EVIDENCE",
        title: "Library rows\nare removed",
        subtitle: "External state and isolation",
        caption: "Oracle AI Database deletes OAMJ_CONCIERGE_MESSAGE and OAMJ_CONCIERGE_RECORDS rows. Red rows show what was removed.",
        seconds: 8),
    Scene(
        visual: .code(repositoryPath, "Map<String, Object> reset()", 22),
        eyebrow: "STEP 0 | JAVA LIFECYCLE ACTION",
        title: "Reset teaching\nrecords",
        subtitle: "MemoryRepository.reset() uses an explicit transaction.",
        caption: "Java deletes mutable teaching records in foreign-key-safe order and preserves the two visitor identities used by both application lanes.",
        seconds: 7),
    Scene(
        visual: .image("step-0-teaching-database.png"),
        eyebrow: "STEP 0 | ORACLE DATABASE EVIDENCE",
        title: "Cold state is\ninspectable",
        subtitle: "Principle: external state and isolation",
        caption: "The AIM_DEMO tables are empty except for Ava and Leo in AIM_DEMO_GUESTS. Browser state is not carrying memory.",
        seconds: 8),

    Scene(
        visual: .image("step-1-library-app.png"),
        eyebrow: "STEP 1 | SCENARIO EVENT",
        title: "Ava states her\npreferences",
        subtitle: "Quiet breakfast, minimal stairs, lantern show, and bullet-point plans.",
        caption: "The app shows two current messages, extracted records, and one seeded prior-visit memory inserted by addMemory().",
        seconds: 8),
    Scene(
        visual: .code(libraryPath, "synchronized Map<String, Object> retain()", 25),
        eyebrow: "STEP 1 | JAVA LIBRARY ACTION",
        title: "Retain typed\nmemory",
        subtitle: "addMessages() extracts records; addMemory() adds the prior episode.",
        caption: "Ollama classifies durable content and Oracle AI Database creates an ALLMINILM embedding for every stored record.",
        seconds: 8),
    Scene(
        visual: .image("step-1-library-database.png"),
        eyebrow: "STEP 1 | ORACLE DATABASE EVIDENCE",
        title: "Messages and\nrecords appear",
        subtitle: "Principle: semantic and episodic retention",
        caption: "OAMJ_CONCIERGE_MESSAGE stores the conversation. OAMJ_CONCIERGE_RECORDS stores typed, identity-scoped records with native vectors.",
        seconds: 8),
    Scene(
        visual: .image("step-1-teaching-app.png"),
        eyebrow: "STEP 1 | SCENARIO EVENT",
        title: "Make lifecycle\nstate explicit",
        subtitle: "The teaching lane adds preferences, an episode, a temporary closure, and three traces.",
        caption: "The UI exposes type, scope, source, version, status, and expiration so the broader lifecycle is easy to explain.",
        seconds: 8),
    Scene(
        visual: .code(repositoryPath, "Map<String, Object> retain()", 25),
        eyebrow: "STEP 1 | JAVA LIFECYCLE ACTION",
        title: "Write selected\nexperience",
        subtitle: "MemoryRepository.retain() uses fixed Java and SQL operations.",
        caption: "Java inserts four durable Ava memories, one temporary route fact, and three de-identified successful traces.",
        seconds: 8),
    Scene(
        visual: .image("step-1-teaching-database.png"),
        eyebrow: "STEP 1 | ORACLE DATABASE EVIDENCE",
        title: "Lifecycle rows\nare added",
        subtitle: "Principle: retain selected state, not an undifferentiated transcript",
        caption: "AIM_DEMO_MEMORIES receives semantic, episodic, and operational records. AIM_DEMO_TRACES receives three shareable outcomes.",
        seconds: 8),

    Scene(
        visual: .image("step-2-library-app.png"),
        eyebrow: "STEP 2 | SCENARIO EVENT",
        title: "Recall before\nthe next turn",
        subtitle: "Ava asks for an accessible quiet morning and rainy evening.",
        caption: "The library ranks only Ava's records, builds a context card, and returns zero Ava records for Leo.",
        seconds: 8),
    Scene(
        visual: .code(libraryPath, "synchronized Map<String, Object> recall(String query)", 24),
        eyebrow: "STEP 2 | JAVA LIBRARY ACTION",
        title: "Scope before\nsemantic ranking",
        subtitle: "search() embeds the request and applies exact identity scope.",
        caption: "The Java path searches four durable record types, then combines ranked memory and recent messages into reusable context.",
        seconds: 8),
    Scene(
        visual: .image("step-2-library-database.png"),
        eyebrow: "STEP 2 | ORACLE DATABASE EVIDENCE",
        title: "Vector search\nuses stored rows",
        subtitle: "Principle: scoped semantic recall",
        caption: "Oracle AI Database generates the query vector and ranks matching OAMJ_CONCIERGE_RECORDS while Ava's scope remains enforced.",
        seconds: 8),
    Scene(
        visual: .image("step-2-teaching-app.png"),
        eyebrow: "STEP 2 | SCENARIO EVENT",
        title: "Reuse a compact\ncontext card",
        subtitle: "The response includes current facts, the rainy episode, and the live closure.",
        caption: "The deliberately imperfect fireworks fact remains visible so the next action can demonstrate correction.",
        seconds: 8),
    Scene(
        visual: .code(repositoryPath, "Map<String, Object> recall(String guestId, String query)", 24),
        eyebrow: "STEP 2 | JAVA LIFECYCLE ACTION",
        title: "Apply scope,\nstatus, and TTL",
        subtitle: "MemoryRepository.recall() records why each memory was selected.",
        caption: "Java reads only active, unexpired, visitor-and-agent-scoped rows and inserts one recall audit row per selected memory.",
        seconds: 8),
    Scene(
        visual: .image("step-2-teaching-database.png"),
        eyebrow: "STEP 2 | ORACLE DATABASE EVIDENCE",
        title: "Recall becomes\nauditable",
        subtitle: "Principle: read before the turn",
        caption: "AIM_DEMO_RECALL_AUDIT records the query, memory identifier, recall reason, visitor, and time for every selected row.",
        seconds: 8),

    Scene(
        visual: .image("step-3-app.png"),
        eyebrow: "STEP 3 | SCENARIO EVENT",
        title: "Ava corrects\nthe extracted fact",
        subtitle: "She wants the lantern show, not the fireworks show.",
        caption: "The UI keeps the old version for audit and activates the guest-confirmed replacement.",
        seconds: 7),
    Scene(
        visual: .code(repositoryPath, "Map<String, Object> correct()", 27),
        eyebrow: "STEP 3 | JAVA ACTION",
        title: "Version the\ncorrection",
        subtitle: "Insert the replacement and supersede the old row in one transaction.",
        caption: "Java locks the current fact, inserts version 2, updates version 1 to SUPERSEDED, and commits both changes atomically.",
        seconds: 8),
    Scene(
        visual: .image("step-3-database.png"),
        eyebrow: "STEP 3 | ORACLE DATABASE EVIDENCE",
        title: "History remains\nvisible",
        subtitle: "Principle: refine through correction, versioning, and provenance",
        caption: "AIM_DEMO_MEMORIES shows the old yellow SUPERSEDED row and the new green ACTIVE version 2 row with guest-correction provenance.",
        seconds: 8),

    Scene(
        visual: .image("step-4-app.png"),
        eyebrow: "STEP 4 | SCENARIO EVENT",
        title: "A temporary path\nclosure ends",
        subtitle: "The garden path reopens, so yesterday's closure must not affect tomorrow.",
        caption: "The UI removes the temporary operational record from active recall while durable preferences remain.",
        seconds: 7),
    Scene(
        visual: .code(repositoryPath, "Map<String, Object> expireOperationalMemory()", 19),
        eyebrow: "STEP 4 | JAVA ACTION",
        title: "Enforce the\nlifecycle",
        subtitle: "Expire the operational record without deleting history.",
        caption: "Java updates rain-route-tonight from ACTIVE to EXPIRED and sets EXPIRES_AT to the database timestamp.",
        seconds: 7),
    Scene(
        visual: .image("step-4-database.png"),
        eyebrow: "STEP 4 | ORACLE DATABASE EVIDENCE",
        title: "Stale state is\nexcluded",
        subtitle: "Principle: TTL and forgetting",
        caption: "AIM_DEMO_MEMORIES highlights the changed EXPIRED row. Future active-and-unexpired queries no longer select it.",
        seconds: 8),

    Scene(
        visual: .image("step-5-app.png"),
        eyebrow: "STEP 5 | SCENARIO EVENT",
        title: "Similar rainy\nreroutes succeed",
        subtitle: "Three de-identified prior traces share a successful pattern.",
        caption: "The app induces one pending rainy-evening-reroute candidate without copying Ava's private details.",
        seconds: 8),
    Scene(
        visual: .code(repositoryPath, "Map<String, Object> dream()", 28),
        eyebrow: "STEP 5 | JAVA ACTION",
        title: "Induce a\ncandidate skill",
        subtitle: "A fixed demonstration rule requires three successful shareable traces.",
        caption: "Java counts matching episodes and inserts generalized JSON instructions with status PENDING.",
        seconds: 8),
    Scene(
        visual: .image("step-5-database.png"),
        eyebrow: "STEP 5 | ORACLE DATABASE EVIDENCE",
        title: "Episodes become\nlearning material",
        subtitle: "Principle: episodic evidence becomes candidate procedural memory",
        caption: "AIM_DEMO_SKILLS receives one green PENDING row with the trigger, generalized steps, source count, and no private guest data.",
        seconds: 8),

    Scene(
        visual: .image("step-6-app.png"),
        eyebrow: "STEP 6 | SCENARIO EVENT",
        title: "A reviewer\napproves the skill",
        subtitle: "The generalized steps are inspected before activation.",
        caption: "The UI changes the induced skill from pending to approved and displays instructions that contain no Ava-specific facts.",
        seconds: 7),
    Scene(
        visual: .code(repositoryPath, "Map<String, Object> approveSkill(String approver)", 22),
        eyebrow: "STEP 6 | JAVA ACTION",
        title: "Gate shared\nprocedural memory",
        subtitle: "approveSkill() activates only the reviewed pending procedure.",
        caption: "Java records the approver and approval time while changing the status from PENDING to APPROVED.",
        seconds: 7),
    Scene(
        visual: .image("step-6-database.png"),
        eyebrow: "STEP 6 | ORACLE DATABASE EVIDENCE",
        title: "Approval has\nprovenance",
        subtitle: "Principle: human governance",
        caption: "AIM_DEMO_SKILLS highlights the changed row with APPROVED status, APPROVED_BY, and APPROVED_AT.",
        seconds: 8),

    Scene(
        visual: .image("step-7-app.png"),
        eyebrow: "STEP 7 | SCENARIO EVENT",
        title: "Leo visits on\na later rainy day",
        subtitle: "He receives the shared procedure but none of Ava's private memory.",
        caption: "The app shows zero Ava memories visible to Leo and the approved rainy-evening-reroute behavior.",
        seconds: 8),
    Scene(
        visual: .code(repositoryPath, "Map<String, Object> nextDay()", 27),
        eyebrow: "STEP 7 | JAVA ACTION",
        title: "Separate private\nand shared reads",
        subtitle: "nextDay() queries private visibility and approved procedures independently.",
        caption: "Java proves that Ava rows under Leo's private scope equal zero, then selects only APPROVED shared skills.",
        seconds: 8),
    Scene(
        visual: .image("step-7-database.png"),
        eyebrow: "STEP 7 | ORACLE DATABASE EVIDENCE",
        title: "No write is\nrequired",
        subtitle: "Principle: safe procedural reuse without private-memory leakage",
        caption: "The final refresh reports zero added, changed, or removed rows. Leo reuses the lesson, not Ava's personal history.",
        seconds: 8),

    Scene(
        visual: .image("step-8-app.png"),
        eyebrow: "STEP 8 | SCENARIO EVENT",
        title: "Reset only the\nquest experience",
        subtitle: "World state remains; Ava's progress, points, badge, and reward audit become empty.",
        caption: "The park graph, spatial map, consented party, quest, and knowledge remain reusable while visitor-specific quest results return to zero.",
        seconds: 8),
    Scene(
        visual: .code(parkRepositoryPath, "Map<String, Object> reset()", 22),
        eyebrow: "STEP 8 | JAVA ACTION",
        title: "Separate world\nand visitor state",
        subtitle: "ParkExperienceRepository.reset() clears only the guest-specific result tables.",
        caption: "Java deletes badges, reward audit, and progress in foreign-key-safe order, then commits without rebuilding the park topology.",
        seconds: 8),
    Scene(
        visual: .image("step-8-database.png"),
        eyebrow: "STEP 8 | ORACLE DATABASE EVIDENCE",
        title: "Quest result\ntables are empty",
        subtitle: "Principle: durable world state is independent from per-visitor experience",
        caption: "AIM_PARK_PROGRESS, AIM_PARK_GUEST_BADGES, and AIM_PARK_REWARD_AUDIT show no rows; graph and knowledge tables remain populated.",
        seconds: 8),

    Scene(
        visual: .image("step-9-app.png"),
        eyebrow: "STEP 9 | SCENARIO EVENT",
        title: "Plan an\naccessible quest route",
        subtitle: "Four permitted graph edges connect the entrance to Lantern Garden.",
        caption: "The map highlights a 675-meter accessible route and keeps the inaccessible Summit Steps visible. Spatial straight-line distance is 592 meters.",
        seconds: 8),
    Scene(
        visual: .code(parkRepositoryPath, "Map<String, Object> plan()", 28),
        eyebrow: "STEP 9 | JAVA ACTION",
        title: "Combine graph\nand Spatial reasoning",
        subtitle: "Topology supplies traversable edges; geometry supplies physical separation.",
        caption: "Java filters closed or inaccessible graph relationships, finds the shortest permitted path, and asks Oracle Spatial for point-to-point distance.",
        seconds: 8),
    Scene(
        visual: .image("step-9-database.png"),
        eyebrow: "STEP 9 | ORACLE DATABASE EVIDENCE",
        title: "Paths carry\nroute policy",
        subtitle: "Principle: graph cost and straight-line distance answer different questions",
        caption: "AIM_PARK_PATHS exposes directed connects edges with distance, accessibility, cover, and status used by AIM_PARK_GRAPH.",
        seconds: 8),

    Scene(
        visual: .image("step-10-app.png"),
        eyebrow: "STEP 10 | SCENARIO EVENT",
        title: "Start Covered\nConstellations",
        subtitle: "Ava and Leo share a consent-bounded party, not a private memory scope.",
        caption: "The app activates three ordered checkpoints and records the quest start while points remain zero.",
        seconds: 8),
    Scene(
        visual: .code(parkRepositoryPath, "Map<String, Object> startQuest()", 28),
        eyebrow: "STEP 10 | JAVA ACTION",
        title: "Create progress\nand audit together",
        subtitle: "startQuest() keeps relationship, authorization, and game state separate.",
        caption: "One transaction creates Ava's active progress and the QUEST_STARTED audit event; party consent is read from member_of graph edges.",
        seconds: 8),
    Scene(
        visual: .image("step-10-database.png"),
        eyebrow: "STEP 10 | ORACLE DATABASE EVIDENCE",
        title: "The start is\ntransactional",
        subtitle: "Principle: shared experience does not erase identity isolation",
        caption: "AIM_PARK_PROGRESS shows step zero and ACTIVE status while AIM_PARK_REWARD_AUDIT records QUEST_STARTED with a zero-point delta.",
        seconds: 8),

    Scene(
        visual: .image("step-11-app.png"),
        eyebrow: "STEP 11 | SCENARIO EVENT",
        title: "Complete three\nquest checkpoints",
        subtitle: "Quiet Café, Covered Atrium, and Lantern Garden finish in order.",
        caption: "The verified UI shows three completed checkpoints, 400 audited points, the Lantern Pathfinder badge, and four reward events.",
        seconds: 8),
    Scene(
        visual: .code(parkRepositoryPath, "Map<String, Object> completeNextStep()", 34),
        eyebrow: "STEP 11 | JAVA ACTION",
        title: "Lock, validate,\nand reward",
        subtitle: "completeNextStep() makes every checkpoint an atomic state transition.",
        caption: "Java locks Ava's progress row, advances only the next ordered step, adds points, and issues the badge inside the final completion transaction.",
        seconds: 9),
    Scene(
        visual: .image("step-11-database.png"),
        eyebrow: "STEP 11 | ORACLE DATABASE EVIDENCE",
        title: "Rewards cannot\ndisagree",
        subtitle: "Principle: transactional gamification",
        caption: "Progress is COMPLETED with 400 points; the badge row and four audit events provide consistent, inspectable evidence.",
        seconds: 8),

    Scene(
        visual: .image("step-12-app.png"),
        eyebrow: "STEP 12 | SCENARIO EVENT",
        title: "Retrieve, then\nexpand the graph",
        subtitle: "Three vector-ranked records produce seven relationship evidence chips.",
        caption: "The app preserves cosine distance, connected places, and quest membership before presenting the grounded park answer.",
        seconds: 8),
    Scene(
        visual: .code(parkRepositoryPath, "Map<String, Object> graphRag(String query)", 34),
        eyebrow: "STEP 12 | JAVA ACTION",
        title: "Make GraphRAG\nevidence visible",
        subtitle: "Vector retrieval identifies places; graph traversal adds explicit context.",
        caption: "Java embeds the request, ranks AIM_PARK_KNOWLEDGE, and follows connects and quest_step edges around every vector hit.",
        seconds: 9),
    Scene(
        visual: .image("step-12-database.png"),
        eyebrow: "STEP 12 | ORACLE DATABASE EVIDENCE",
        title: "Knowledge has\nnative vectors",
        subtitle: "Principle: semantic relevance plus explainable relationships",
        caption: "AIM_PARK_KNOWLEDGE stores grounded content with 384-dimensional vectors; AIM_PARK_GRAPH supplies the connected place and quest structure.",
        seconds: 8),

    Scene(
        visual: .recap,
        eyebrow: "GOVERNED CONTINUAL LEARNING",
        title: "Retain · Recall\nReuse · Refine",
        subtitle: "Durable experience changes future context without retraining model weights.",
        caption: "Oracle AI Database keeps agent memory scoped and auditable, then combines graph, Spatial, vectors, and transactions for a safe playable experience.",
        seconds: 9)
]

let fm = FileManager.default
try fm.createDirectory(at: build, withIntermediateDirectories: true)
for file in [output, poster, srt, vtt] where fm.fileExists(atPath: file.path) {
    try fm.removeItem(at: file)
}

let red = NSColor(calibratedRed: 0.78, green: 0.18, blue: 0.12, alpha: 1)
let gold = NSColor(calibratedRed: 0.92, green: 0.66, blue: 0.28, alpha: 1)
let teal = NSColor(calibratedRed: 0.12, green: 0.51, blue: 0.49, alpha: 1)
let ink = NSColor(calibratedRed: 0.06, green: 0.12, blue: 0.14, alpha: 1)
let muted = NSColor(calibratedWhite: 0.70, alpha: 1)
let panel = NSColor(calibratedWhite: 0.13, alpha: 1)

func rounded(_ rect: NSRect, radius: CGFloat, color: NSColor) {
    color.setFill()
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
}

func drawText(
    _ value: String,
    _ rect: NSRect,
    font: NSFont,
    color: NSColor,
    alignment: NSTextAlignment = .left,
    spacing: CGFloat = 4
) {
    let paragraph = NSMutableParagraphStyle()
    paragraph.alignment = alignment
    paragraph.lineSpacing = spacing
    paragraph.lineBreakMode = .byWordWrapping
    value.draw(
        with: rect,
        options: [.usesLineFragmentOrigin, .usesFontLeading],
        attributes: [
            .font: font,
            .foregroundColor: color,
            .paragraphStyle: paragraph
        ])
}

func badge(_ label: String, x: CGFloat, y: CGFloat, color: NSColor) {
    let size = (label as NSString).size(
        withAttributes: [.font: NSFont.boldSystemFont(ofSize: 22)])
    rounded(
        NSRect(x: x, y: y, width: size.width + 34, height: 44),
        radius: 22,
        color: color.withAlphaComponent(0.22))
    drawText(
        label,
        NSRect(x: x + 17, y: y + 8, width: size.width, height: 30),
        font: .boldSystemFont(ofSize: 22),
        color: color)
}

func sourceExcerpt(path: URL, marker: String, lines: Int) -> String {
    let text = (try? String(contentsOf: path, encoding: .utf8))
        ?? "Source file unavailable"
    let sourceLines = text.components(separatedBy: .newlines)
    let start = sourceLines.firstIndex(where: { $0.contains(marker) }) ?? 0
    let lower = max(0, start - 2)
    let upper = min(sourceLines.count, lower + lines)
    return sourceLines[lower..<upper].enumerated().map {
        String(format: "%3d  %@", lower + $0.offset + 1, $0.element)
    }.joined(separator: "\n")
}

func drawCode(path: String, marker: String, lines: Int) {
    let rect = NSRect(x: 650, y: 140, width: 1190, height: 680)
    rounded(rect, radius: 22, color: NSColor(calibratedWhite: 0.095, alpha: 1))
    rounded(
        NSRect(x: rect.minX, y: rect.minY, width: rect.width, height: 60),
        radius: 22,
        color: NSColor(calibratedWhite: 0.16, alpha: 1))
    for (index, color) in [NSColor.systemRed, .systemYellow, .systemGreen].enumerated() {
        color.setFill()
        NSBezierPath(
            ovalIn: NSRect(
                x: rect.minX + 24 + CGFloat(index) * 31,
                y: rect.minY + 21,
                width: 16,
                height: 16)).fill()
    }
    drawText(
        "memory/\(path)",
        NSRect(x: rect.minX + 125, y: rect.minY + 15, width: rect.width - 150, height: 34),
        font: .monospacedSystemFont(ofSize: 20, weight: .semibold),
        color: muted)
    drawText(
        sourceExcerpt(
            path: memoryRoot.appendingPathComponent(path),
            marker: marker,
            lines: lines),
        NSRect(x: rect.minX + 30, y: rect.minY + 82, width: rect.width - 60, height: rect.height - 104),
        font: .monospacedSystemFont(ofSize: lines > 24 ? 17 : 19, weight: .regular),
        color: NSColor(calibratedRed: 0.84, green: 0.89, blue: 0.91, alpha: 1),
        spacing: lines > 24 ? 4 : 6)
}

func drawImage(filename: String) {
    let path = captureRoot.appendingPathComponent(filename)
    guard let image = NSImage(contentsOf: path) else {
        drawText(
            "Verified application capture unavailable",
            NSRect(x: 650, y: 360, width: 1190, height: 80),
            font: .boldSystemFont(ofSize: 34),
            color: .white,
            alignment: .center)
        return
    }
    let frame = NSRect(x: 635, y: 135, width: 1215, height: 690)
    rounded(frame, radius: 22, color: panel)
    let scale = min(
        (frame.width - 18) / image.size.width,
        (frame.height - 18) / image.size.height)
    let size = NSSize(width: image.size.width * scale, height: image.size.height * scale)
    let target = NSRect(
        x: frame.midX - size.width / 2,
        y: frame.midY - size.height / 2,
        width: size.width,
        height: size.height)
    image.draw(
        in: target,
        from: NSRect(origin: .zero, size: image.size),
        operation: .sourceOver,
        fraction: 1,
        respectFlipped: true,
        hints: [.interpolation: NSImageInterpolation.high])
}

func drawRecordTypes() {
    let items = [
        ("preference", "semantic", "likes, dislikes, desired responses"),
        ("fact", "semantic", "stable current user facts"),
        ("guideline", "procedural-like", "future response instruction"),
        ("memory", "semantic or episodic", "durable catch-all by content")
    ]
    for (index, item) in items.enumerated() {
        let x = 650 + CGFloat(index % 2) * 595
        let y = 155 + CGFloat(index / 2) * 250
        rounded(NSRect(x: x, y: y, width: 550, height: 210), radius: 22, color: panel)
        drawText(
            item.0,
            NSRect(x: x + 28, y: y + 26, width: 490, height: 42),
            font: .monospacedSystemFont(ofSize: 27, weight: .bold),
            color: gold)
        drawText(
            "Closest type: \(item.1)",
            NSRect(x: x + 28, y: y + 83, width: 490, height: 38),
            font: .boldSystemFont(ofSize: 23),
            color: .white)
        drawText(
            item.2,
            NSRect(x: x + 28, y: y + 135, width: 490, height: 48),
            font: .systemFont(ofSize: 22),
            color: muted)
    }
    rounded(NSRect(x: 650, y: 665, width: 1145, height: 120), radius: 22, color: teal)
    drawText(
        "message",
        NSRect(x: 685, y: 694, width: 180, height: 38),
        font: .monospacedSystemFont(ofSize: 26, weight: .bold),
        color: .white)
    drawText(
        "Recent or working context from OAMJ_CONCIERGE_MESSAGE, not a durable record in OAMJ_CONCIERGE_RECORDS.",
        NSRect(x: 880, y: 690, width: 870, height: 65),
        font: .systemFont(ofSize: 22),
        color: .white)
}

func drawTitleVisual() {
    rounded(NSRect(x: 710, y: 220, width: 970, height: 430), radius: 34, color: panel)
    drawText(
        "JAVA\nAGENT LOOP",
        NSRect(x: 790, y: 330, width: 280, height: 130),
        font: .boldSystemFont(ofSize: 35),
        color: .white,
        alignment: .center)
    drawText(
        "↔",
        NSRect(x: 1100, y: 360, width: 150, height: 80),
        font: .boldSystemFont(ofSize: 60),
        color: gold,
        alignment: .center)
    drawText(
        "ORACLE AI\nDATABASE",
        NSRect(x: 1280, y: 330, width: 300, height: 130),
        font: .boldSystemFont(ofSize: 35),
        color: .white,
        alignment: .center)
    drawText(
        "READ BEFORE     ·     WRITE AFTER",
        NSRect(x: 850, y: 520, width: 700, height: 46),
        font: .boldSystemFont(ofSize: 24),
        color: teal,
        alignment: .center)
}

func drawRecap() {
    for (index, label) in ["RETAIN", "RECALL", "REUSE", "REFINE"].enumerated() {
        let x = 640 + CGFloat(index) * 290
        rounded(
            NSRect(x: x, y: 350, width: 240, height: 145),
            radius: 72,
            color: index % 2 == 0 ? red : teal)
        drawText(
            label,
            NSRect(x: x + 20, y: 400, width: 200, height: 45),
            font: .boldSystemFont(ofSize: 26),
            color: .white,
            alignment: .center)
    }
    drawText(
        "SCOPED · VERSIONED · EXPIRABLE · AUDITABLE",
        NSRect(x: 590, y: 565, width: 1280, height: 60),
        font: .boldSystemFont(ofSize: 26),
        color: gold,
        alignment: .center)
    drawText(
        "GRAPH · SPATIAL · VECTOR · TRANSACTIONS",
        NSRect(x: 590, y: 625, width: 1280, height: 60),
        font: .boldSystemFont(ofSize: 26),
        color: teal,
        alignment: .center)
}

func imageFor(index: Int) -> NSImage {
    let scene = scenes[index]
    let image = NSImage(size: NSSize(width: width, height: height))
    image.lockFocusFlipped(true)
    ink.setFill()
    NSBezierPath(rect: NSRect(x: 0, y: 0, width: width, height: height)).fill()
    rounded(NSRect(x: 0, y: 0, width: 18, height: height), radius: 0, color: red)
    drawText(
        scene.eyebrow,
        NSRect(x: 70, y: 52, width: 1160, height: 34),
        font: .boldSystemFont(ofSize: 21),
        color: red)
    drawText(
        scene.title,
        NSRect(x: 70, y: 110, width: 520, height: 220),
        font: .boldSystemFont(ofSize: index == 0 ? 70 : 51),
        color: .white,
        spacing: 3)
    drawText(
        scene.subtitle,
        NSRect(x: 75, y: 350, width: 500, height: 190),
        font: .systemFont(ofSize: 25),
        color: muted,
        spacing: 7)

    switch scene.visual {
    case .title:
        badge("JAVA 21", x: 75, y: 570, color: teal)
        badge("OJDBC AGENT MEMORY", x: 75, y: 635, color: red)
        badge("ORACLE AI DATABASE", x: 75, y: 700, color: gold)
        drawTitleVisual()
    case .recordTypes:
        drawRecordTypes()
    case .image(let filename):
        drawImage(filename: filename)
    case .code(let path, let marker, let lines):
        drawCode(path: path, marker: marker, lines: lines)
    case .recap:
        drawRecap()
    }

    rounded(
        NSRect(x: 125, y: 865, width: 1670, height: 155),
        radius: 22,
        color: NSColor(calibratedWhite: 0.02, alpha: 0.95))
    drawText(
        scene.caption,
        NSRect(x: 175, y: 895, width: 1570, height: 100),
        font: .boldSystemFont(ofSize: 27),
        color: .white,
        alignment: .center,
        spacing: 7)
    drawText(
        String(format: "%02d / %02d", index + 1, scenes.count),
        NSRect(x: 1690, y: 52, width: 150, height: 28),
        font: .monospacedSystemFont(ofSize: 18, weight: .medium),
        color: muted,
        alignment: .right)
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
        milliseconds % 1000)
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
    encoding: .utf8)
try (vttText.trimmingCharacters(in: .newlines) + "\n").write(
    to: vtt,
    atomically: true,
    encoding: .utf8)

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
let adaptor = AVAssetWriterInputPixelBufferAdaptor(
    assetWriterInput: input,
    sourcePixelBufferAttributes: attributes)
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
    while !input.isReadyForMoreMediaData {
        Thread.sleep(forTimeInterval: 0.002)
    }
    var buffer: CVPixelBuffer?
    CVPixelBufferCreate(
        kCFAllocatorDefault,
        width,
        height,
        kCVPixelFormatType_32ARGB,
        attributes as CFDictionary,
        &buffer)
    guard let pixelBuffer = buffer else {
        fatalError("Cannot allocate pixel buffer")
    }
    CVPixelBufferLockBaseAddress(pixelBuffer, [])
    let context = CGContext(
        data: CVPixelBufferGetBaseAddress(pixelBuffer),
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: CVPixelBufferGetBytesPerRow(pixelBuffer),
        space: colorSpace,
        bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue)!
    context.draw(
        rendered[sceneIndex],
        in: CGRect(x: 0, y: 0, width: width, height: height))
    CVPixelBufferUnlockBaseAddress(pixelBuffer, [])
    adaptor.append(
        pixelBuffer,
        withPresentationTime: CMTime(value: Int64(frame), timescale: fps))
}
input.markAsFinished()
let semaphore = DispatchSemaphore(value: 0)
writer.finishWriting { semaphore.signal() }
semaphore.wait()
guard writer.status == .completed else {
    throw writer.error ?? NSError(domain: "MemoryVideo", code: 1)
}

print(String(format: "Created %@ (%.1f seconds)", output.lastPathComponent, cursor))
print("Created poster, SRT, and WebVTT caption assets")
