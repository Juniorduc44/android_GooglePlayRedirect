# Model manifest (download — do not commit large weights)

## Runtime targets

| Path | Runtime | Notes |
|---|---|---|
| Primary | llama.cpp + GGUF (JNI on Android; llama-cpp-python on desktop) | Shared core; broad GGUF |
| Alternate | LiteRT-LM + Gemma-3n E2B / E4B | Google MTP path; Kotlin/C++ |
| Third | ONNX Runtime GenAI / ExecuTorch | Needs genai_config.json + tokenizer.json |

## Planned assets

| ID | Type | Approx size | Use | Status |
|---|---|---|---|---|
| Free path (deep-translator) | HTTP MT (not on-device) | 0 (lib only) | Desktop translate without API key | **active** |
| `tinyllama` (Ollama) | Chat GGUF via Ollama | ~600 MB | Desktop local chat translate | optional install |
| Opus-MT en-es (TBD file) | Marian MT | ~50–300 MB | Pair MT (quality-per-MB win) | planned |
| Opus-MT en-tl (`Helsinki-NLP/opus-mt-en-tl`) | Marian MT | ~50–300 MB | Filipino / Tagalog (desktop local HF) | wired |
| NLLB-200-distilled-600M Q4 | GGUF | ~400 MB+ | Many-to-many | planned |
| Gemma-3n E2B / E4B | LiteRT-LM | per Google guide | Alternate runtime only | optional later |

## RAM / thermal guidance (for packaging docs)

- ~1B params @ Q4 ≈ ~1 GB RAM; short prompts on mid-range 8 GB phones are realistic.
- Heat is the real ceiling: sustained decode throttles within minutes — prefer short translate bursts.
- Do not ship multi-GB GGUF in git; first-run download + checksums.

Checksums and download URLs will be added when assets are frozen.
