# Interactive AI blog walkthrough

This directory contains the reproducible source and publication assets for the narrated blog walkthrough.

```bash
cd a2ui_agui_mcpapps_mcptoolkit/video
./build-video.swift
./verify-video.swift
```

The macOS build uses Swift, AppKit, AVFoundation, and the system `say` command. It creates:

- `interactive-ai-walkthrough.mp4` — 1920×1080 H.264 video with narration and burned subtitles
- `interactive-ai-walkthrough-poster.png` — blog poster frame
- `interactive-ai-walkthrough.srt` — YouTube-ready subtitles
- `interactive-ai-walkthrough.vtt` — captions used by the HTML5 blog embed

No `.env` file, password, wallet, token, or private connection string is read by the video build.

Narration files and both caption formats retain the canonical names `A2UI`, `AG-UI`, and `MCP Apps`. The renderer applies pronunciation aliases only to the temporary speech-synthesis input.
