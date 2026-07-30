# Memory blog walkthrough video

This directory builds a silent, subtitle-led 1920x1080 MP4 for the memory article.
The source scenes read the actual Java repository files. The application and
database scenes use current Chrome captures from the verified Oracle AI Database
run. Each numbered scenario shows the observed behavior, relevant Java method,
database evidence, and memory principle.

```bash
cd memory/video
./build-video.swift
./verify-video.swift
```

Generated publication assets:

- `memory-agent-walkthrough.mp4`
- `memory-agent-walkthrough-poster.png`
- `memory-agent-walkthrough.srt`
- `memory-agent-walkthrough.vtt`

The MP4 intentionally contains no audio track. Captions are burned into the
video and also published as SRT and WebVTT sidecars.
