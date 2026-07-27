# REFERENCES — translation models & decisions

## Decisions log

| Date | Decision | Why |
|---|---|---|
| 2026-07-27 | **Three** Android runtimes (not TFLite-centric): llama.cpp, LiteRT-LM, ONNX GenAI/ExecuTorch | 2026 mobile research |
| 2026-07-27 | llama.cpp + GGUF primary (JNI/NDK) | Most flexible OSS; same core as `llama-cpp-python` on desktop; more integration work accepted |
| 2026-07-27 | LiteRT-LM as alternate | Powers Gemini Nano (Chrome, Pixel Watch); MTP ~Apr 2026 >2× decode on mobile GPU; start with Gemma-3n E2B/E4B |
| 2026-07-27 | ONNX GenAI / ExecuTorch third | Viable; needs model + tokenizer + `genai_config.json` / `tokenizer.json` packaging |
| 2026-07-27 | Opus-MT / Marian for fixed pairs | Best quality-per-MB (often 50–300 MB/pair) vs chat 1–3B LLMs |
| 2026-07-27 | NLLB-200 distilled (~600M) for many-to-many | Better than repurposed chat LLM when pairs unknown |
| 2026-07-27 | **Heat** is first-class ceiling | Sustained mobile decode throttles within minutes; stress-test sustained use |
| 2026-07-27 | RAM claims model-dependent | ~1B Q4 ≈ 1 GB; 1.2B Q4_K_M on 8 GB mid-range OK for short prompts; “8–12 GB for 3B+” is high-side for good quant |
| 2026-07-27 | xAI 403 “no credits” ≠ invalid key | Surface billing URL; default model `grok-4.5` |
| 2026-07-27 | Free path = deep-translator until on-device MT | Unblocks Translator without cloud credits |
| 2026-07-27 | No release until translate works | User requirement |

## Android runtime notes (detail)

### 1. llama.cpp + GGUF (chosen)

- Broad GGUF support; open-source; full control of threads / memory / battery.
- Desktop: `llama-cpp-python` for prototyping with the same weights.
- Android: cross-compile + JNI wrap; thermal and battery tuning are **ours**.

### 2. LiteRT-LM (Google)

- Successor to MediaPipe LLM Inference API.
- Gemini Nano deployment surface (Chrome, Pixel Watch).
- Multi-Token Prediction feature (~April 2026): **>2× faster decode** on mobile GPUs.
- Kotlin / C++ APIs; starter models: **Gemma-3n E2B / E4B** (MediaPipe-compatible).

### 3. ONNX Runtime GenAI / ExecuTorch

- Push model + tokenizer files to device.
- Generate `genai_config.json` and `tokenizer.json` for the `generate()` API.
- More packaging friction than GGUF for our workflow.

## Cloud model IDs (xAI)

Prefer current docs: https://docs.x.ai/docs/models

- `grok-4.5` — default flagship  
- `grok-4.3` — strong mid  
- `grok-3-mini` — cheaper/faster if available on account  

Chat endpoint: `POST https://api.x.ai/v1/chat/completions`  
Billing (403 no credits): https://console.x.ai/

## Local / mobile assets

See `models/MANIFEST.md`.

## goobleTranslator

Original: https://github.com/Juniorduc44/goobleTranslator  
Reference copy: `reference/goobleTranslator.py`
