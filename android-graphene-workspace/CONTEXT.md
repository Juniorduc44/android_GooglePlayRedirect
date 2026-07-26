# Current Project(s)

Update this file every time your focus shifts. This is the file the AI
reads to know what "right now" looks like — keep it current, not historical
(historical/background stuff belongs in `REFERENCES.md`).

## Workspace purpose

A master workspace for building Android tools and utilities, developed and
tested against **GrapheneOS** as the primary target OS. Tools should also be
reasonably portable to stock AOSP/Android where possible, but GrapheneOS is
the design target — see `docs/graphene-os-notes.md` for the constraints
that shape every decision here.

## What we are building

**php-usd-converter** (active tool) — CustomTkinter desktop GUI and Android
app that convert Philippine Peso (PHP) ↔ US Dollar (USD) with a live
exchange rate, offline fallback, and a swap button for direction.

Also tracked:

**google-play-redirect** — an Android app/utility that intercepts or opens
Google Play Store links and redirects them to the browser (or another
FOSS-friendly source) instead of the Play Store app. Primary use case is
GrapheneOS devices with no Play Store / sandboxed Play, where market
URLs should not force a dead-end or GMS dependency.

## Target device / environment

- OS: GrapheneOS (version: TBD — confirm on device)
- Device: Pixel (TBD model) — GrapheneOS only supports Pixel hardware
- Root: no root
- Google Play compatibility layer: not assumed installed
- Dev connection: USB debugging over ADB (device not yet attached in this env)

## What good looks like

- Works without any GMS/Play Services dependency
- Fails gracefully when a permission is revoked mid-session
- No hardcoded assumptions about root or an unlocked bootloader
- Clear, minimal output — a developer tool, not a consumer app, unless the
  project says otherwise
- Play/`market://` and `play.google.com` style links open in a browser (or
  user-chosen handler) instead of requiring the Play Store app

## What to avoid

- Any dependency on Firebase, Google Maps SDK, Play Integrity/SafetyNet, or
  other GMS-only APIs unless the task explicitly targets a device with
  sandboxed Google Play
- Assuming root is available
- Broad filesystem scanning that ignores Storage Access Framework / scoped
  storage
- Silent failures — always surface permission/security errors to the user
- Shipping anything that reintroduces a hard Play Store requirement

## Active sub-projects

| Project folder | What it is | Status |
|---|---|---|
| `projects/_template/` | Template to copy for new tools | n/a |
| `projects/php-usd-converter/` | PHP ↔ USD (swap + rate refresh on swap) **v1.1.1** | shipped |
| `projects/google-play-redirect/` | Redirect Play Store links to browser | in progress |

To start a new tool: copy `projects/_template/` to `projects/<your-tool-name>/`
and fill in its `CONTEXT.md`, or run `scripts/new-project.sh <name>`.
