# Project: google-play-redirect

## What this tool does

On-device app: Play/market links → browser. **Debug mode** logs every intent
this app receives and lets the user save the log anywhere via SAF.

## Status

- [x] Shipped **v1.1.1** (debug mode + startup crash fix)

## Release artifacts

- Latest: `dist/google-play-redirect-v1.1.1.apk`
- Prior: `dist/google-play-redirect-v1.1.0.apk`, `v1.0.0.apk`
- `versionName` 1.1.1 / `versionCode` 10101

## Debug honesty

Cannot monitor other apps’ Google login UIs without root/adb. Logs only
what is delivered to this package.
