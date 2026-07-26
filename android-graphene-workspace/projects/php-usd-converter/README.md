# php-usd-converter

Converts **Philippine Peso (PHP)** to **US Dollar (USD)** using a live
exchange rate, with an offline fallback.

| Build | Version |
|---|---|
| Android APK | **v1.0.0** (`versionCode` 1) |
| Package ID | `com.juniorduc44.phpusdconverter` |
| Min / target SDK | 26 / 34 |

## Outputs

| Artifact | Path |
|---|---|
| **Release APK v1.0.0** | `dist/php-usd-converter-v1.0.0.apk` |
| Alias | `dist/v1.0.0.apk` |
| Desktop GUI | `app.py` |

## Desktop app (CustomTkinter)

Logic verified in this environment (live rate fetch + conversion). The GUI
needs a real display (not available headless/Docker without X11).

```bash
cd android-graphene-workspace/projects/php-usd-converter
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
# Debian/Ubuntu: sudo apt install python3-tk
python app.py
```

## Android APK v1.0.0

Native Kotlin port of the same converter:

- Live rate: `https://api.exchangerate-api.com/v4/latest/PHP` → `rates.USD`
- Offline fallback: `0.0175`
- Dark UI (Material), no Google Play Services
- Internet permission only

### Install on device (ADB)

```bash
adb install -r dist/php-usd-converter-v1.0.0.apk
# or
adb install -r dist/v1.0.0.apk
```

Enable **Install unknown apps** / sideload if prompted (GrapheneOS: allow for
the installer you use).

### Rebuild the APK

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/home/coder/android_GooglePlayRedirect/tools/android-sdk
cd android
./gradlew assembleRelease
cp app/build/outputs/apk/release/app-release.apk ../dist/php-usd-converter-v1.0.0.apk
cp ../dist/php-usd-converter-v1.0.0.apk ../dist/v1.0.0.apk
```

## Project layout

```text
php-usd-converter/
├── README.md
├── CONTEXT.md
├── app.py                 # desktop CustomTkinter app
├── requirements.txt
├── dist/
│   ├── php-usd-converter-v1.0.0.apk
│   └── v1.0.0.apk
└── android/               # Kotlin Android Studio / Gradle project
    ├── app/
    └── gradlew
```

## Notes

- Desktop CustomTkinter **cannot** be packaged as a real APK directly; the
  Android app is a native port with the same behavior.
- No GMS / Play Integrity / Firebase.
- Release is signed with a project-local keystore under `android/app/` for
  sideload builds (not a Play Store upload key).
