# Project: google-play-redirect

Scoped context for this specific tool. Inherits all rules and constraints
from the workspace-level `AGENTS.md` — do not repeat those here, only what's
specific to this project.

## What this tool does

Intercepts or handles Google Play Store links (`market://`,
`https://play.google.com/...`) and opens them in a browser (or another
user-chosen FOSS-friendly destination) instead of launching the Play Store
app. Aimed at GrapheneOS and other no-GMS setups where the Play Store is
missing, sandboxed, or deliberately unused.

## Type

- [x] On-device app
- [ ] CLI / host-side script
- [ ] ADB automation script
- [ ] Other: ___

(Host-side ADB helpers may be added later for install/test.)

## Root required?

- [x] No (default — prefer this)
- [ ] Yes — reason: ___

## Status

- [ ] Not started
- [x] In progress
- [ ] Working / testing
- [ ] Shipped

## What good looks like (specific to this tool)

- Registers as a handler for Play Store / market intents without GMS
- Redirects to a browser with a usable URL (web Play page, or configurable
  alternative such as Aurora/F-Droid search if we add that later)
- Works with GrapheneOS network/sensor toggles revoked where irrelevant
- Minimal permissions; no contacts/storage unless explicitly needed
- Installable via sideload / Obtainium / F-Droid-style distribution (no Play
  requirement to obtain the tool itself)

## What to avoid (specific to this tool)

- Embedding Play Integrity, Play Core, or billing libraries
- Hard dependency on a specific browser package name without fallback
- Silent drop of unhandled URL schemes — show a clear error or chooser
- Assuming the Play Store package is installed for any part of the flow

## Open questions / blockers

- Preferred redirect target: plain `play.google.com` in browser, Aurora Store
  deep link, F-Droid search, or user-configurable?
- Should this be a tiny intent-filter-only app, or include a settings UI?
- Min SDK / target Pixel generation for GrapheneOS testing?
- Device not yet visible via `adb devices` in the current dev environment
