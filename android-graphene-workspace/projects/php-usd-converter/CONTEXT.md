# Project: php-usd-converter

Scoped context for this specific tool. Inherits all rules and constraints
from the workspace-level `AGENTS.md` — do not repeat those here, only what's
specific to this project.

## What this tool does

A small desktop GUI that converts PHP amounts to USD. Fetches a live
exchange rate on startup; if the request fails, uses a fixed approximate
fallback rate so the converter still works offline.

## Type

- [x] CLI / host-side script
- [ ] On-device app
- [ ] ADB automation script
- [x] Other: Desktop GUI (CustomTkinter)

## Root required?

- [x] No (default — prefer this)
- [ ] Yes — reason: ___

## Status

- [ ] Not started
- [ ] In progress
- [ ] Working / testing
- [x] Shipped (v1.0.0 APK)

## Release artifacts

- `dist/php-usd-converter-v1.0.0.apk` (also `dist/v1.0.0.apk`)
- `applicationId`: `com.juniorduc44.phpusdconverter`
- `versionName`: `1.0.0` / `versionCode`: `1`

## What good looks like (specific to this tool)

- Clear PHP input → USD output with formatting
- Live rate when online; silent fallback when offline
- Invalid input shown in-UI without crashing
- Own folder with README so the tool is self-contained

## What to avoid (specific to this tool)

- Hard crash on network failure
- Requiring API keys for the free public rate endpoint
- Coupling this tool to Android/ADB unless explicitly requested later

## Open questions / blockers

- GUI needs a display; headless Docker/code-server may not show the window
  without X11 forwarding or a local desktop run
- Whether to add a “Refresh rate” button or multi-currency pairs later
