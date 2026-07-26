# google-play-redirect

GrapheneOS-friendly handler that opens **Google Play Store links in the
browser** instead of requiring the Play Store app.

| Build | Value |
|---|---|
| Android APK | **v1.0.0** (`versionCode` 10000) |
| Package ID | `com.juniorduc44.playredirect` |
| Min / target SDK | 26 / 34 |

## Why this exists (GrapheneOS)

On GrapheneOS, the Play Store is often **missing** or only present as an
optional sandboxed install. Apps and links still fire `market://` and
`play.google.com` intents. Without a handler, those actions fail or
dead-end.

This app:

1. Registers for `market://`, `play.google.com`, and `market.android.com`
2. Maps them to a normal **HTTPS** Play web URL
3. Opens that URL with your **browser** (chooser)

### Important: Google **login** vs Play **links**

| Need | What helps |
|---|---|
| Open / survive Play Store deep links | **This app** |
| In-app **Google Sign-In** (GMS APIs) | GrapheneOS **sandboxed Google Play** (optional, from GrapheneOS App Store) |

Redirecting links does **not** install Play Services. Use both when you
want browser fallbacks *and* Google login inside apps.

## Features (v1.0.0)

- No GMS / Play Core / Integrity libraries
- No `INTERNET` permission (browser does the network work)
- Transparent `RedirectActivity` (no flashy UI on each link)
- Main screen with setup steps + GrapheneOS notes
- Test button, shortcuts to default-apps / app settings

## APK

```text
dist/google-play-redirect-v1.0.0.apk
```

```bash
adb install -r dist/google-play-redirect-v1.0.0.apk
```

After install: when Android asks which app should open a Play link, pick
**Play Redirect** and **Always** if you want it as default.

## Project layout

```text
google-play-redirect/
├── README.md
├── CONTEXT.md
├── VERSION / VERSIONING.md
├── dist/
│   └── google-play-redirect-v1.0.0.apk
└── android/          # Gradle / Kotlin
```

## Rebuild

```bash
cd android
export PLAY_REDIRECT_KEYSTORE=app/release.keystore   # local, not in git
# + password env vars per VERSIONING / build.gradle.kts
./gradlew assembleRelease
cp app/build/outputs/apk/release/app-release.apk \
  ../dist/google-play-redirect-v$(cat ../VERSION).apk
```
