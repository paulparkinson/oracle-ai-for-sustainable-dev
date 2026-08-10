# Memory blog walkthrough video

This directory builds the latest silent, subtitle-led 1920x1080 MP4 for the
memory article. A dark neon circuit visual system matches the browser app and
blog while preserving accessible caption contrast. Its 16 numbered steps follow
the complete Ava and Leo theme-park story: governed memory, accessible route
planning, consent-bounded quest play, GraphRAG, and privacy-first AR memory.

Every step contains three verified views:

1. the relevant live application state;
2. the source method called by that action; and
3. the database content or change, including an explicit read-only explanation
   when the step does not write rows.

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
video and also published as SRT and WebVTT sidecars. Current Chrome captures are
kept as build inputs under `.build/captures-latest/` and are not published as
standalone article assets.
