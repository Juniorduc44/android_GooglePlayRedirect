# google-play-redirect

Redirect Google Play Store links to the browser (or another FOSS-friendly
handler) instead of the Play Store app.

## Status

In progress — project scaffolded inside the GrapheneOS-first workspace.
App source not yet implemented.

## Context

See `CONTEXT.md` in this folder, plus workspace-level:

- `../../AGENTS.md` — rules and GrapheneOS constraints
- `../../CONTEXT.md` — active workspace focus
- `../../REFERENCES.md` — links and decisions

## Dev notes

- Primary target: GrapheneOS on Pixel hardware, no root, no GMS assumed
- Verify device: `adb devices`
- Language default for on-device code: Kotlin
