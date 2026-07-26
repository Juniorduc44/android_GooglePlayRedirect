# Project: google-play-redirect

## What this tool does

On-device Android app that intercepts Google Play Store links
(`market://`, `https://play.google.com/...`) and opens them in the system
browser as HTTPS. Aimed at GrapheneOS where Play Store is absent or
sandboxed.

**User goal:** reduce friction on GrapheneOS when apps open Play links, and
pair with GrapheneOS sandboxed Play for actual Google login APIs.

## Type

- [x] On-device app

## Root required?

- [x] No

## Status

- [x] In progress / first build **v1.0.0**

## Release artifacts

- `dist/google-play-redirect-v1.0.0.apk`
- `applicationId`: `com.juniorduc44.playredirect`
- `versionName`: `1.0.0` / `versionCode`: `10000`

## What good looks like

- Handles market + play.google.com without GMS
- Browser chooser if no default browser
- Clear in-app explanation of redirect vs sandboxed Play for Sign-In
- Minimal permissions

## What to avoid

- Play Integrity / billing / GMS SDKs
- Assuming Play Store package is installed
- Claiming this alone “fixes Google login” without sandboxed Play

## Open questions

- Later: optional redirect targets (Aurora Store, F-Droid search)
- Later: per-host toggles in settings
