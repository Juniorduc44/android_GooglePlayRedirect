# php-usd-converter

Converts **Philippine Peso (PHP)** ↔ **US Dollar (USD)** using a live
exchange rate, with an offline fallback and a **swap** control for direction.

| Build | Value |
|---|---|
| Desktop toolkit | **v1.8.1** (Spec · Dex import + locks) |
| Android APK | **v1.8.1** Spec + Dex locks (`versionCode` 10801) |
| Package ID | `com.juniorduc44.phpusdconverter` |
| Min / target SDK | 26 / 34 |
| APK naming | `php-usd-converter-vMAJOR.MINOR.PATCH.apk` only |

## Versioning SOP

**Global (whole workspace):**  
[`../../VERSIONING.md`](../../VERSIONING.md)

**App-specific notes:** [`VERSIONING.md`](./VERSIONING.md)

Current version file: [`VERSION`](./VERSION) → `1.8.1`

### Changelog

#### Checkpoint 2026-07-31 (research only)

- **Wallet / passkey work parked** — not in app menus (see `tools/passkey-research/CHECKPOINT.md`)
- Product remains **v1.8.1**: Convert · Travel · Weight · Temp · Spec · Chain · Settings

#### v1.8.1

- **Spec · From DexScreener**: load trending/volume/meme/RWA tokens
- **Field locks**: keep supply (or mcap/price) from live data; uncheck to type what-if values
- Estimated supply from mcap÷price when Dex omits raw supply
- Updated plan: [`docs/SPEC_PLAN.md`](./docs/SPEC_PLAN.md)
- **No Wallet section** in the shipped app

#### v1.8.0

- **Spec** section: buy / sell / speculate calculators (mcap→price, spend→items, holdings×target)
- Shortcuts: `1.5b` / `50m` / `250k`; copy price & items between panels
- Plan: [`docs/SPEC_PLAN.md`](./docs/SPEC_PLAN.md)
- **Removed Wallet** (no keystore / private keys); Chain remains markets viewer only
- Plan for faster load + cleaner UI: [`docs/UI_PERF_PLAN.md`](./docs/UI_PERF_PLAN.md)

#### v1.7.0

- **Wallet** section (later removed): local EOA on Robinhood Chain (4663)
- Password-encrypted keystore; balance via public RH RPC; copy address
- **Faster Blockchain**: fewer DexScreener seeds, 90s cache, auto-load only once

#### v1.6.3

- **Sandwich / hamburger menu** (top-right) — all tools + Settings live in the menu
- **No top tab strip** — full screen used by the active section
- Title bar shows current section only (more room for in-tab controls later)

#### v1.6.2

- **Modern Chain UI** (desktop + Android): coin cards, LIVE pill, cleaner hierarchy
- **Market view dropdown**: Top 10 volume · Trending boosts · Trending momentum · Memecoins · RWA · My contracts
- **Set as default** view (persisted on desktop `user_settings.json` / Android SharedPreferences)
- DexScreener boosts + momentum fetchers

#### v1.6.1

- **Android Chain tab** (was missing from v1.6.0 APK — desktop-only by mistake)
- Same Robinhood Chain tracker: 5 RWAs, top memecoins, custom contract, self-test
- Auto-refresh when you open the **Chain** tab

#### v1.6.0

- **Blockchain tab (desktop):** Robinhood Chain (4663) price tracker via DexScreener
- ≥5 RWA/stock tokens (NVDA, TSLA, AAPL, GOOGL, MSFT) with contracts + prices
- Top 10 memecoins by 24h volume; custom contract track; self-test + CLI probe
- Plan: `docs/ROBINHOOD_CHAIN_PLAN.md` (passkey wallet Phase C — not shipped)
- Local tool: `tools/browser-harness` clone docs in repo `tools/README.md`
- **Translator:** still unfinished / not reliable
- **Note:** Android UI for Chain was not included until **v1.6.1**

#### v1.5.1

- **Temp tab:** food / oven **°C ↔ °F** with adjacent ⇄ switch (desktop + Android)
- Same UI pattern as Weight / currency switches
- **Translator:** still unfinished / not reliable end-to-end — do not treat as done

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
