# Workspace Architecture

How this workspace is laid out and why, so it scales past a single project
without turning into one giant file an AI has to re-read every time.

## Three-layer idea

1. **Workspace layer** (`AGENTS.md`, `CLAUDE.md`, `CONTEXT.md`,
   `REFERENCES.md`, `docs/`) — the stuff that's true no matter which tool
   you're building: identity, GrapheneOS constraints, toolchain, and
   accumulated decisions/links. Loaded every session.
2. **Project layer** (`projects/<name>/`) — one folder per tool or utility.
   Each has its own `CONTEXT.md` scoped to just that project, so you're not
   dragging every past project's details into a new conversation.
3. **Prompt layer** (`prompts/`) — reusable prompt patterns for the kinds
   of tasks that come up repeatedly (new tool scaffold, permission audit,
   GrapheneOS-compat review, debugging an ADB issue).

## Why this split

If everything lived in one `CONTEXT.md`, it would grow forever and every
conversation would load irrelevant history. Splitting it means:

- Starting a new tool = copy `projects/_template/`, fill in a short
  `CONTEXT.md`, done. The workspace-level `AGENTS.md` constraints still
  apply automatically.
- An AI working on `projects/permission-auditor/` only needs
  `AGENTS.md` + `CONTEXT.md` (workspace) + `projects/permission-auditor/CONTEXT.md`
  — not the details of three other tools you built last month.
- `REFERENCES.md` and `docs/` act as long-term memory that doesn't need to
  be re-explained per project, but also doesn't clutter the "what am I
  doing right now" file.

## Adding a new project

```bash
./scripts/new-project.sh my-new-tool
```

This copies `projects/_template/` to `projects/my-new-tool/` and pre-fills
the name. Then point your AI session at that folder plus the workspace
root.

## When to promote something from a project to the workspace layer

If you notice yourself repeating the same constraint or reference link
across two or more projects, move it up into `AGENTS.md`,
`docs/graphene-os-notes.md`, or `REFERENCES.md` instead of copy-pasting it
into every project's `CONTEXT.md`.
