# CONTEXT — translation runtime (keep current)

## Active product

Desktop multi-tool app **php-usd-converter** Translator tab.  
Mobile on-device path is **planned** (see `docs/MOBILE_LLM_PLAN.md`).

## Preferred engines (priority)

1. **Free (no API key)** — deep-translator / public MT endpoint — works without credits  
2. **Ollama local** small chat model — offline if user installed Ollama  
3. **xAI Grok** — cloud; requires team **credits** on console.x.ai  
4. **OpenAI / Hugging Face** — user API tokens  

Future (after Phase 1–2):

5. **Opus-MT pair** on-device / desktop local  
6. **NLLB-200 distilled GGUF** via llama.cpp for many-to-many  
7. Small chat GGUF for phonics only  

## On-device (future Android)

| Priority | Asset |
|---|---|
| High | Opus-MT pairs for primary languages (en↔es, en↔tl/fil, …) — 50–300 MB/pair |
| Medium | NLLB-200 distilled ~600M GGUF for many-to-many |
| Low | Tiny chat GGUF only for phonics / free-form |

**Runtime target:** llama.cpp + GGUF via JNI/NDK (Python: `llama-cpp-python` for desktop).  
**Alternate:** LiteRT-LM + Gemma-3n E2B/E4B (Google path, MTP speedups).  
**Third:** ONNX Runtime GenAI / ExecuTorch if packaging forces it.

## Device limits to respect

- **Heat first:** significant throttle within minutes of sustained inference — design for short bursts, cool-downs, not continuous chat.  
- **RAM second:** ~1B Q4 ≈ 1 GB; do not document “need 8–12 GB for every 3B” for well-quantized builds.  
- Prefer models that stay cool under **short** bursts (translate one paragraph).  
- Default chat quant: **Q4_K_M** or similar; Opus-MT as specialized MT.

## Active languages

Full list: `translator/languages.py` (goobleTranslator set).  
First-class mobile pairs (to pack first): English, Spanish, Filipino/Tagalog, Japanese, Mandarin Chinese.
