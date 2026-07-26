# Android Tools Workspace (GrapheneOS-first)

A master workspace for building Android tools and utilities, designed
around **GrapheneOS** as the primary target OS, and built so any AI —
Claude, Claude Code, ChatGPT, Gemini, Cursor, whatever — gets the same
context and follows the same rules.

## Quick start

1. Point your AI tool at this folder (open a Claude Project with these
   files as Project Knowledge, run `claude` from inside this folder for
   Claude Code, or open it in Cursor/whatever editor you're using).
2. It should read `AGENTS.md` first, then `CONTEXT.md`, then
   `REFERENCES.md`.
3. Update `CONTEXT.md` whenever what you're actively building changes.
4. Start a new tool with `./scripts/new-project.sh <name>`, then fill in
   `projects/<name>/CONTEXT.md`.

## Why it's structured this way

This follows a three-layer pattern: workspace-wide rules and constraints
(rarely change) → per-project context (changes per tool) → per-task prompts
(change every message). See `docs/architecture.md` for the full
explanation.

## Folder guide

| File/Folder | Purpose |
|---|---|
| `AGENTS.md` | Universal rules + GrapheneOS constraints, for any AI |
| `CLAUDE.md` | Thin pointer file so Claude/Claude Code auto-loads it |
| `CONTEXT.md` | What's being built right now, workspace-wide |
| `REFERENCES.md` | Links, decisions log, examples |
| `docs/graphene-os-notes.md` | Deep GrapheneOS technical reference |
| `docs/android-tooling-stack.md` | Toolchain/setup checklist |
| `docs/architecture.md` | How/why the workspace is laid out this way |
| `prompts/prompt-template.md` | Reusable 5-part prompt patterns |
| `projects/_template/` | Copy this for every new tool |
| `scripts/new-project.sh` | Scaffolds a new project folder |

## GrapheneOS, in one paragraph

GrapheneOS is a hardened AOSP fork, Pixel-only, with no Google Play
Services by default, its own per-app network/sensor toggles, scoped
storage/contacts spoofing, a stricter memory allocator, and no root by
design. Every tool built here should assume that environment unless a
project explicitly says otherwise — see `AGENTS.md` for the full list of
constraints and `docs/graphene-os-notes.md` for the detail behind them.
