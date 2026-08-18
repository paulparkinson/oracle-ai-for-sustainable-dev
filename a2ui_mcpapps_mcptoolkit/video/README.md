# A2UI and MCP Apps blog walkthrough

This directory contains the reproducible source and publication assets for the
silent, captioned walkthrough embedded in `../blog.html`. The video demonstrates
native A2UI and portable MCP Apps over the same Oracle-governed supply-chain
workflow, including examples in Gemini Enterprise, ChatGPT, and Claude.

Build and verify on macOS:

```bash
cd a2ui_mcpapps_mcptoolkit/video
swift build-video.swift
swift verify-video.swift
../../scripts/audit_video.sh \
  interactive-ai-walkthrough.mp4 \
  interactive-ai-walkthrough.srt \
  ../blog.html
```

The build creates:

- `interactive-ai-walkthrough.mp4`: silent 1920x1080 H.264 video with burned captions
- `interactive-ai-walkthrough-poster.png`: blog poster frame
- `interactive-ai-walkthrough.srt`: downloadable and YouTube-ready subtitles
- `interactive-ai-walkthrough.vtt`: captions used by the HTML5 embed

Authentic product screenshots are rendered inside a controlled frame with
animated cursor movement and click pulses. Browser chrome is cropped, and no
secret, private endpoint, account, project, agent, or database identifier is
included. The scripted animation is safer and more reproducible than publishing
an unedited desktop recording.
