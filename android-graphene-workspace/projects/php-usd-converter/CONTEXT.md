# Project: php-usd-converter

Scoped context for this specific tool. Inherits all rules and constraints
from the workspace-level `AGENTS.md` — do not repeat those here, only what's
specific to this project.

## What this tool does

A small desktop GUI and Android app that convert PHP amounts to USD. Fetches
a live exchange rate on startup; if the request fails, uses a fixed
approximate fallback rate so the converter still works offline.

## Type

- [x] CLI / host-side script
- [x] On-device app
- [ ] ADB automation script
- [x] Other: Desktop GUI (CustomTkinter)

## Root required?

- [x] No (default — prefer this)
- [ ] Yes — reason: ___

## Status

- [ ] Not started
- [ ] In progress
- [ ] Working / testing
- [x] Shipped (**v1.0.0** APK)

## Release artifacts

- **Only** `dist/php-usd-converter-v1.0.0.apk` (long name; no short alias)
- `applicationId`: `com.juniorduc44.phpusdconverter`
- `versionName`: `1.0.0` / `versionCode`: `1` (historical first release)
- **Versioning SOP:** `VERSIONING.md` (SemVer major / minor / patch)
- **Current VERSION file:** `1.0.0`

## What good looks like (specific to this tool)

- Clear PHP input → USD output with formatting
- Live rate when online; silent fallback when offline
- Invalid input shown in-UI without crashing
- Own folder with README; single long-form APK name per version
- Releases follow `VERSIONING.md` exactly

## What to avoid (specific to this tool)

- Hard crash on network failure
- Requiring API keys for the free public rate endpoint
- Duplicate short APK names in `dist/` (e.g. `v1.0.0.apk`)
- Skipping `versionCode` bumps or reusing an old `versionCode`

## Open questions / blockers

- Whether to add a “Refresh rate” button or multi-currency pairs later
