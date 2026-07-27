# php-usd-converter

Converts **Philippine Peso (PHP)** ↔ **US Dollar (USD)** using a live
exchange rate, with an offline fallback and a **swap** control for direction.

| Build | Value |
|---|---|
| Desktop toolkit | **v1.5.0** (includes Translator) |
| Android APK | **v1.4.0** convert/travel/weight (`versionCode` 10400) |
| Package ID | `com.juniorduc44.phpusdconverter` |
| Min / target SDK | 26 / 34 |
| APK naming | `php-usd-converter-vMAJOR.MINOR.PATCH.apk` only |

## Versioning SOP

**Global (whole workspace):**  
[`../../VERSIONING.md`](../../VERSIONING.md)

**App-specific notes:** [`VERSIONING.md`](./VERSIONING.md)

Current version file: [`VERSION`](./VERSION) → `1.5.0`

### Changelog

#### v1.5.0 (desktop)

- **Translator tab** (from [goobleTranslator](https://github.com/Juniorduc44/goobleTranslator)): translate + phonics
- AI backends: **Free offline (default, no API key)**, **Ollama**, **xAI/Grok**, **OpenAI**, **Hugging Face**
- Secure `secrets.json` for API keys (gitignored); configure in Settings
- Docs: `docs/TRANSLATOR_PLAN.md`, `docs/CUSTOMTKINTER.md`, `docs/MOBILE_LLM_PLAN.md`, `llm-workflow/`
- Script: `scripts/setup_ollama_tiny.sh` for a small local model
- **No release** until translate works end-to-end (xAI needs credits at console.x.ai)

#### v1.4.0

- **Weight tab:** convert **lb ↔ kg ↔ g** (adjacent unit cycle switch)
- Result text size setting applies to weight results too

#### v1.3.2

- Travel switches sit **next to their labels**: `Distance (km):` + `⇄ mi`, `Trip cost (₱ PHP):` + `⇄ USD`

#### v1.3.1

- **Travel currency switch:** choose PHP or USD for trip cost inside the Travel tab (independent of Convert)

#### v1.3.0

- **Settings tab:** result text size Small / Medium / Large / Extra large
- Larger default result fonts; Travel cost-per-km shown more prominently
- Travel panel scrolls so results are not clipped
- Preference saved (desktop: `user_settings.json`, Android: SharedPreferences)

#### v1.2.0

- **Travel tab:** distance (km/mi switch), trip cost, cost per km or mile
- Subtle unit equivalents in parentheses (mi ↔ km)
- Subtle opposite-currency amounts using the live FX rate (PHP ↔ USD)
- Desktop (`app.py`) and Android both have Convert | Travel tabs

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
| **Latest release APK** | `dist/php-usd-converter-v1.4.0.apk` |
| Prior | `v1.2.0`, `v1.1.1`, `v1.0.0` |
| Desktop GUI | `app.py` |

## Desktop app (CustomTkinter)

Tabs: **Convert** · **Travel** · **Weight** · **Translator** · **Settings**

```bash
cd android-graphene-workspace/projects/php-usd-converter
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
# Debian/Ubuntu: sudo apt install python3-tk
python app.py
```

### Translator / AI backends

| Backend | Needs | Notes |
|---|---|---|
| **Free offline (default)** | Network; `deep-translator` | No API key; plain MT only (no phonics) |
| **Ollama (local LLM)** | Ollama + small model | `./scripts/setup_ollama_tiny.sh tinyllama` |
| **xAI / Grok** | API key **and** team credits | Preferred cloud; 403 “no credits” ≠ bad key → https://console.x.ai/ |
| **OpenAI** | API key | Compatible with original goobleTranslator workflow |
| **Hugging Face** | Token + model id | Inference API |

Keys are stored only in **`secrets.json`** (gitignored, chmod 600).  
On-device MT plan (llama.cpp / Opus-MT / LiteRT-LM): `docs/MOBILE_LLM_PLAN.md` + `llm-workflow/`.

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
