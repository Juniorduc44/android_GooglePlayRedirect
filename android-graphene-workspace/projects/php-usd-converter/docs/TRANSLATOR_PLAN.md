# Plan: Translator tab (from goobleTranslator)

## Source study: `goobleTranslator` (Juniorduc44)

Cloned to `vendor/goobleTranslator/` and copied for reference under
`reference/goobleTranslator.py`.

### Original behavior

| Piece | Implementation |
|---|---|
| UI | Standalone CustomTkinter window (not class-based) |
| Input | `CTkTextbox` (source text) |
| Output | `CTkTextbox` (translation / phonics) |
| Target language | `CTkOptionMenu` of ~36 languages |
| Translate | OpenAI **Completions** `text-davinci-003`: `translate '{text}' into {lang}` |
| Phonics | Second menu + button: “Write out pronunciation… Phonetic Alphabet” |
| Auth | `OPENAI_API_KEY` via `python-dotenv` |
| Clear | Delete output textbox |

### Issues to fix when porting

- Deprecated Completions API / retired `text-davinci-003`
- Global widgets + duplicated language lists
- Dead code (`a = True` always) and inconsistent variable names
- API key only from env file; no in-app secure entry
- No offline path

## Product goals

1. Tab named **Translator** in the multi-tool CustomTkinter app.
2. Same core UX: text in → language → translate → optional phonics → clear.
3. **Selectable AI backend** with clear defaults:
   - **Free offline (default)** — deep-translator (network, no API key) so
     Translate works without cloud credits until on-device Opus-MT/GGUF ships.
   - **Ollama (local LLM)** — small model the user installs locally (cannot
     ship multi‑GB weights in git; setup script + auto-detect).
   - **Hugging Face** — token + model id via Inference API.
   - **Cloud API key** — prefer **xAI (SpaceXAI / Grok)** OpenAI-compatible
     endpoint; also support OpenAI-compatible keys for gooble users.
     Note: xAI needs **team credits** at console.x.ai (403 ≠ bad key).
4. Secure key storage (never commit keys).
5. Document CustomTkinter widgets used.

## Architecture

```
php-usd-converter/
  app.py                      # shell + tabs (Convert, Travel, Weight, Translator, Settings)
  translator/
    __init__.py
    languages.py              # shared language list
    backends.py               # Ollama / xAI / OpenAI / Hugging Face
    secrets_store.py          # secrets.json (chmod 600), never git-tracked
  docs/
    TRANSLATOR_PLAN.md        # this file
    CUSTOMTKINTER.md          # widget notes from official docs
  scripts/
    setup_ollama_tiny.sh      # pull a small Ollama model
  reference/
    goobleTranslator.py       # original for comparison
```

### Backend interface

```python
class TranslationBackend(Protocol):
    name: str
    def available(self) -> tuple[bool, str]: ...
    def translate(self, text: str, target_lang: str) -> str: ...
    def phonetics(self, text: str, phonetic_lang: str) -> str: ...
```

| Backend | How | Models |
|---|---|---|
| `offline` | deep-translator (Google free endpoint) | n/a — plain MT only, no phonics |
| `ollama` | HTTP `POST /api/chat` on `127.0.0.1:11434` | default `tinyllama` (or user pick) |
| `xai` | OpenAI-compatible `https://api.x.ai/v1` | default `grok-4.5` |
| `openai` | OpenAI chat completions | e.g. `gpt-4o-mini` |
| `huggingface` | HF Inference API (chat/completions or text-generation) | user-set model id |

Mobile on-device plan (llama.cpp / Opus-MT / LiteRT-LM): `docs/MOBILE_LLM_PLAN.md`.

Translation prompt (all chat backends):

```text
Translate the following text into {target_lang}.
Reply with only the translation, no quotes or commentary.

{text}
```

Phonics prompt:

```text
Write the pronunciation of the following text using a {phonetic_lang}
phonetic alphabet / romanization that a speaker of {phonetic_lang} can read.
Reply with only the pronunciation.

{text}
```

### Secrets (`secrets.json`, gitignored)

```json
{
  "active_backend": "offline",
  "ollama_base_url": "http://127.0.0.1:11434",
  "ollama_model": "tinyllama",
  "xai_api_key": "",
  "xai_model": "grok-4.5",
  "openai_api_key": "",
  "openai_model": "gpt-4o-mini",
  "hf_token": "",
  "hf_model": "meta-llama/Llama-3.2-3B-Instruct"
}
```

- Written with `os.chmod(..., 0o600)` when possible.
- Keys only shown masked in UI (`••••` + last 4).
- Settings tab (or Translator “AI provider” panel) to set backend + keys.

### UI (Translator tab)

Using existing dark CustomTkinter theme:

1. Provider row: `CTkOptionMenu` backends + status label (online/offline)
2. Source `CTkTextbox`
3. Target language `CTkOptionMenu` + **Translate** button
4. Output `CTkTextbox`
5. Phonics language `CTkOptionMenu` + **Phonics** button
6. **Clear** button
7. Link to configure keys (inline expander or Settings subsection)

### Android

**Out of scope for v1.5.0** (Ollama/API embedding is desktop-first).  
Android package keeps Convert/Travel/Weight only; README notes Translator is desktop.

### Version

SemVer **minor**: `1.4.0` → **`1.5.0`** (desktop VERSION / docs).  
Android APK version may stay 1.4.0 until a later mobile port.

## Implementation order

1. Docs (this + CUSTOMTKINTER.md) ✓  
2. `translator/` package (languages, secrets, backends)  
3. Wire **Translator** tab in `app.py`  
4. Settings: AI provider + keys  
5. `scripts/setup_ollama_tiny.sh` + requirements  
6. Smoke-test without network (ollama unavailable → clear error)

## Risks

| Risk | Mitigation |
|---|---|
| “Bake Ollama into app” not feasible in git | Setup script + docs; optional model download |
| HF multi-lang models vary | Chat-style LLM prompt works for any language list |
| Blocking UI on network | Run translate in `threading.Thread`, update UI via `after()` |
| Key leakage | gitignore + chmod + never log full keys |
