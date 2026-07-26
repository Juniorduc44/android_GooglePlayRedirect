# google-play-redirect

GrapheneOS-friendly handler that opens **Google Play Store links in the
browser** instead of requiring the Play Store app.

| Build | Value |
|---|---|
| Android APK | **v1.2.0** (`versionCode` 10200) |
| Package ID | `com.juniorduc44.playredirect` |
| Min / target SDK | 26 / 34 |

## Debug mode (v1.1.0)

1. Open **Play Redirect**
2. Toggle **Enable debug logging**
3. Trigger Play links (or tap **Test redirect**)
4. Preview the log on screen
5. Tap **Save debug log…** → Android file picker → pick **any folder/path** you want
6. Optional: Clear / Refresh

### What is logged

- Full inbound intents (`market://`, play.google.com, …)
- Referrer / calling package when Android exposes them
- URI, query params, extras (sensitive-looking keys redacted)
- Redirect resolution and open success / failure

### What is **not** logged (platform limit)

Google Sign-In UI **inside other apps** is not visible to a normal app on
GrapheneOS without root or `adb logcat`. Debug mode records everything that
**hits this app** so you can see who opened a Play link and what we did.

## Why this exists

On GrapheneOS, Play Store is often missing. Apps still fire `market://` /
`play.google.com` intents. This app maps them to HTTPS and opens the browser.

**Google login:** still use GrapheneOS optional **sandboxed Google Play** for
GMS Sign-In APIs. This app handles **links**, not Play Services.

## APK

```text
dist/google-play-redirect-v1.2.0.apk
```

```bash
adb install -r dist/google-play-redirect-v1.2.0.apk
```

## Changelog

### v1.2.0
- Explain/fix **other apps → this app** (not a missing app permission)
- Handler status: who owns `market://` / play.google.com
- **Test as external link** (system chooser, like real apps)
- **Open by default** settings shortcut
- Higher intent-filter priority + package visibility queries

### v1.1.1
- **Crash fix:** replace MaterialSwitch (requires Material3 theme) with SwitchCompat so the app starts under Theme.MaterialComponents

### v1.1.0
- Debug mode toggle
- Intent / redirect logging to on-device file
- Save log via system document picker (user chooses location)
- Clear + preview UI

### v1.0.0
- market / play.google.com → browser
- Help screen + setup shortcuts
