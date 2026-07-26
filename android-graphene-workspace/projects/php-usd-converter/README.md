# php-usd-converter

Converts **Philippine Peso (PHP)** ↔ **US Dollar (USD)** using a live
exchange rate, with an offline fallback and a **swap** control for direction.

| Build | Value |
|---|---|
| Android APK | **v1.1.1** (`versionCode` 10101) |
| Package ID | `com.juniorduc44.phpusdconverter` |
| Min / target SDK | 26 / 34 |
| APK naming | `php-usd-converter-vMAJOR.MINOR.PATCH.apk` only |

## Versioning SOP

**Global (whole workspace):**  
[`../../VERSIONING.md`](../../VERSIONING.md)

**App-specific notes:** [`VERSIONING.md`](./VERSIONING.md)

Current version file: [`VERSION`](./VERSION) → `1.1.1`

### Changelog

#### v1.1.1

- **Swap refreshes rate:** ⇄ re-fetches the live rate (same as app start)
- Patch over v1.1.0 swap feature

#### v1.1.0

- **Swap direction:** ⇄ button toggles PHP → USD and USD → PHP
- Labels, convert action, result currency, and rate line update with direction
- Desktop `app.py` gets the same swap behavior

#### v1.0.0

- Initial Android release: PHP → USD converter with live rate and offline fallback
- Desktop CustomTkinter app (`app.py`) included in the same project folder

## Outputs

| Artifact | Path |
|---|---|
| **Latest release APK** | `dist/php-usd-converter-v1.1.1.apk` |
| Prior | `dist/php-usd-converter-v1.0.0.apk` |
| Desktop GUI | `app.py` |

## Desktop app (CustomTkinter)

```bash
cd android-graphene-workspace/projects/php-usd-converter
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
# Debian/Ubuntu: sudo apt install python3-tk
python app.py
```

## Android APK

- Live rate on start; **⇄ Swap** also re-fetches the rate
- Offline fallback: `0.0175` (USD per 1 PHP)
- Bidirectional PHP ↔ USD
- No Google Play Services; Internet permission only

### Install

```bash
adb install -r dist/php-usd-converter-v1.1.1.apk
```

### Rebuild

```bash
cd android && ./gradlew assembleRelease
VER=$(cat ../VERSION)
cp app/build/outputs/apk/release/app-release.apk \
  "../dist/php-usd-converter-v${VER}.apk"
```
