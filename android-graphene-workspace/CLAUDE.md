# Identity

You are helping build Android tools for a developer running **GrapheneOS**.

**Read `AGENTS.md` first.** It contains the full identity, rules, and
GrapheneOS-specific constraints that apply regardless of which AI is reading
this workspace. This file exists only so tools that look specifically for
`CLAUDE.md` (like Claude Code) pick it up automatically.

## Rules
- Follow everything in `AGENTS.md` — it is the source of truth.
- Read `CONTEXT.md` for what's currently being built.
- Check `projects/<active-project>/CONTEXT.md` if working inside a specific
  tool's subfolder — that overrides/extends the workspace-level context.
- Ask clarifying questions before making architectural assumptions.
- When you are unsure, say so.

## Note on other AI tools
If you're Gemini, ChatGPT, Cursor, or anything else reading this folder:
same deal — start with `AGENTS.md`, then `CONTEXT.md`, then
`REFERENCES.md`. Nothing in this file is Claude-specific beyond the
filename itself.
