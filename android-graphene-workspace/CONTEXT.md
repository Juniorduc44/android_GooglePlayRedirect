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

**google-play-redirect** (active) — Android app that intercepts Google Play
Store / `market://` links and opens them in the browser so GrapheneOS users
are not stuck without a Play Store handler. Complements (does not replace)
optional sandboxed Google Play for in-app Google Sign-In.

Also tracked:

**php-usd-converter** — PHP ↔ USD converter (desktop + Android), shipped
**v1.1.1**.

## Target device / environment

- OS: GrapheneOS (Pixel)
- Root: no root
- Google Play compatibility layer: optional / user-installed for Sign-In;
  **not** required for this redirector
- Dev connection: ADB when available

## What good looks like

- Works without any GMS/Play Services dependency in *this* app
- Play/`market://` and `play.google.com` links open in a browser
- Honest UX about redirect vs sandboxed Play for Google login
- Fails gracefully when no browser is available

## What to avoid

- Bundling GMS / Play Integrity / billing
- Assuming root or unlocked bootloader
- Silent failures on unhandled URIs

## Active sub-projects

| Project folder | What it is | Status |
|---|---|---|
| `projects/_template/` | Template to copy for new tools | n/a |
| `projects/google-play-redirect/` | Play links → browser + debug log export | **v1.1.0** |
| `projects/php-usd-converter/` | PHP ↔ USD converter | shipped **v1.1.1** |
