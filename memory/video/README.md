# Memory blog walkthrough video

This directory builds a silent, subtitle-led 1920x1080 MP4 for the memory article.
The source scenes read the actual Java repository files. The application and
database scenes use current Chrome captures from the verified Oracle AI Database
run. All thirteen numbered scenario states, steps 0 through 12, show the
observed behavior, relevant Java method, database evidence, and memory
principle. The final five steps cover SQL Property Graph, Oracle Spatial,
consent-bounded party relationships, transactional gamification, and GraphRAG.

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
