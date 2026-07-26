# php-usd-converter

Converts **Philippine Peso (PHP)** to **US Dollar (USD)** using a live
exchange rate, with an offline fallback.

| Build | Value |
|---|---|
| Android APK | **v1.0.0** (`versionCode` 1) |
| Package ID | `com.juniorduc44.phpusdconverter` |
| Min / target SDK | 26 / 34 |
| APK naming | `php-usd-converter-vMAJOR.MINOR.PATCH.apk` only |

## Versioning SOP

**Global (whole workspace):**  
[`../../VERSIONING.md`](../../VERSIONING.md)

**App-specific notes:** [`VERSIONING.md`](./VERSIONING.md)

| Change type | Version bump | Example |
|---|---|---|
| Major | `MAJOR` +1, reset minor/patch | `v1.0.0` → `v2.0.0` |
| Minor | `MINOR` +1, reset patch | `v1.0.0` → `v1.1.0` |
| Patch | `PATCH` +1 | `v1.0.0` → `v1.0.1` |

Current version file: [`VERSION`](./VERSION) → `1.0.0`

### Changelog

#### v1.0.0

- Initial Android release: PHP → USD converter with live rate and offline fallback
- Desktop CustomTkinter app (`app.py`) included in the same project folder

## Outputs

| Artifact | Path |
|---|---|
| **Release APK** | `dist/php-usd-converter-v1.0.0.apk` |
| Desktop GUI | `app.py` |

One APK name only — no short aliases (`v1.0.0.apk`).

## Desktop app (CustomTkinter)

```bash
cd android-graphene-workspace/projects/php-usd-converter
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
# Debian/Ubuntu: sudo apt install python3-tk
python app.py
```

## Android APK

Native Kotlin port of the same converter:

- Live rate: `https://api.exchangerate-api.com/v4/latest/PHP` → `rates.USD`
- Offline fallback: `0.0175`
- Dark UI (Material), no Google Play Services
- Internet permission only

### Install on device

Download / sideload:

```text
dist/php-usd-converter-v1.0.0.apk
```

Or via ADB:

```bash
adb install -r dist/php-usd-converter-v1.0.0.apk
```

### Rebuild (see VERSIONING.md for full checklist)

```bash
# After bumping versionName / versionCode in android/app/build.gradle.kts
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=../../../../tools/android-sdk   # adjust if needed
cd android
./gradlew assembleRelease
VER=$(cat ../VERSION)
cp app/build/outputs/apk/release/app-release.apk \
  "../dist/php-usd-converter-v${VER}.apk"
```

## Project layout

```text
php-usd-converter/
├── README.md
├── VERSIONING.md          # SOP for major / minor / patch releases
├── VERSION                # current X.Y.Z (no "v" prefix)
├── CONTEXT.md
├── app.py
├── requirements.txt
├── dist/
│   └── php-usd-converter-v1.0.0.apk
└── android/
```

## Notes

- Desktop CustomTkinter cannot be packaged as an APK; Android is a native port.
- No GMS / Play Integrity / Firebase.
- Keystore is local / not in git; signed `dist/` APK is the shippable artifact.
