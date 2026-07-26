# Prompt Template — Five-Part Framework

Reference: identity and context are mostly already handled by `AGENTS.md`
and `CONTEXT.md`. For most tasks in this workspace you only need to add
**Task**, **Constraints**, and **Output Format** — identity/context are
inherited.

## The five parts

1. **Identity** — usually skip; already covered by `AGENTS.md`. Only
   restate it if you need the AI to shift role for a one-off (e.g. "review
   this as a security auditor" instead of "build this").
2. **Task** — the specific ask. Clear action + defined scope.
3. **Context** — anything specific to this task not already in
   `CONTEXT.md`/`REFERENCES.md`.
4. **Constraints** — what to avoid, this time.
5. **Output Format** — shape of the result.

## Filled examples for this workspace

### New CLI tool

```
Task: Build a Python CLI tool that connects over ADB and lists every
installed app's currently granted dangerous permissions.

Context: Runs against a GrapheneOS device with no root. Should use
`adb shell dumpsys package <pkg>` output, not require any special adb
server flags.

Constraints: No third-party Python packages beyond the standard library
plus `subprocess`. Must handle a device with no apps returned gracefully.
Do not assume the user is root.

Output Format: A single `permission_audit.py` file with a short usage
comment at the top, plus a 3-line explanation of how to run it.
```

### Reviewing GrapheneOS compatibility

```
Task: Review this Kotlin snippet for anything that will silently break on
GrapheneOS.

Context: [paste code]. This is meant to run on a device with no Google
Play Services and per-app network/sensor toggles enabled.

Constraints: Flag issues, do not rewrite the whole file unless asked.

Output Format: A short bullet list — one bullet per issue, each with the
line/area affected and the GrapheneOS-specific reason it's a problem.
```

### Debugging an ADB issue

```
Task: Help me figure out why `adb shell pm grant` is failing on this
package.

Context: [paste the exact command and error output]. Device is GrapheneOS,
non-root, USB debugging enabled, device shows "device" (not
"unauthorized") in `adb devices`.

Constraints: Don't suggest rooting as the first troubleshooting step.

Output Format: Ordered list of things to check, most likely cause first.
```

## Chunking reminder

If a project is bigger than one prompt can handle (e.g. "build a full
permission-management app"), break it the way Lesson 1.3 describes:
outline → review → one component at a time → review → next component.
Don't ask for the whole app in one shot.
