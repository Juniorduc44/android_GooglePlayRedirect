# Mobile on-device translation plan (2026)

**Status:** planning + foundation built on desktop; Android NDK runtime not shipped yet.  
**Do not release** until a path translates reliably (cloud *or* free offline HTTP *or* on-device MT).

## Goals

1. Translator works without depending solely on paid cloud credits.
2. Mobile path is **efficient** for translation (not a bloated general chat LLM).
3. Reuse the same **file/folder workflow** as the GrapheneOS workspace (`AGENTS.md` /
   `CLAUDE.md` / `CONTEXT.md`) so an on-device model (and human/AI helpers) share one
   context pack for prompts, languages, and constraints.
4. Keep commits small and reversible.

## What the research implies (accepted 2026 picture)

> Not a TFLite-centric story. On Android in 2026 there are **three real runtime
> options**; for this Python + folder workflow, **llama.cpp** is the practical pick.

### Runtimes on Android (three real options)

| Rank | Runtime | Reality |
|---|---|---|
| **1 (practical for us)** | **llama.cpp + GGUF via JNI/NDK** | Most flexible open-source runtime; broad GGUF support. More Android integration work (JNI/NDK): threading, memory, and **battery tuning left to you**. Mature Python bindings on the dev side (`llama-cpp-python`) for prototyping; **cross-compile / JNI-wrap the same core** for Android. |
| **2** | **Google LiteRT-LM** (successor to MediaPipe LLM Inference API) | Powers Gemini Nano in Chrome and Pixel Watch. **Multi-Token Prediction (MTP)** added ~April 2026 delivers **>2× faster decode** on mobile GPUs. Kotlin/C++ APIs. Google’s guide points at **MediaPipe-compatible Gemma-3n** variants (**E2B**, **E4B**) as starting models. |
| **3** | **ONNX Runtime GenAI / ExecuTorch** | Viable but more setup: model **plus** tokenizer files on device; extra processing to generate `genai_config.json` and `tokenizer.json` for ONNX Runtime’s `generate()` API. |

**Decision for this project:**

- Prototype with **llama.cpp** on desktop (`llama-cpp-python`).
- Ship Android via **JNI wrap of llama.cpp** when assets are ready.
- LiteRT-LM remains a documented alternate if we later standardize on Gemma-3n only.
- ONNX GenAI / ExecuTorch only if packaging needs force that path.

### Models: dedicated MT beats generic chat LLMs

A generic small LLM (Gemma / Llama / Qwen 1–3B) is **overkill and often worse
quality-per-megabyte** for translation specifically.

| Need | Model class | Size / notes |
|---|---|---|
| Fixed, known language pairs | **Opus-MT / MarianMT** | Often **50–300 MB per pair** — trained for MT; best efficiency win for “efficiently complex handler” |
| Many-to-many arbitrary pairs | **NLLB-200 distilled ~600M** → GGUF/ONNX | Better fit than a general chat LLM |
| Chatty rewrite / phonics / free-form | Small chat GGUF (TinyLlama, Phi, Gemma Q4) | Only when MT can’t cover the task |

**Decision:**

- **Phase A (pairs we care about):** Opus-MT pair models (en↔es, en↔tl/fil if available, en↔zh, …).
- **Phase B (open many-to-many):** NLLB-200-distilled GGUF via llama.cpp.
- **Phonics / open instruction:** small GGUF chat model *or* cloud when online.

### Device constraints (RAM vs heat)

| Topic | Reality |
|---|---|
| **Heat (primary ceiling)** | The single biggest constraint on mobile LLMs is often **not** model size or RAM — it is **heat**. Significant performance drops within **minutes** of sustained inference; varies a lot by device thermal design. **Stress-test under real, sustained use**, not just a quick demo. |
| **RAM (model-dependent)** | ~**1B Q4 ≈ ~1 GB** RAM. Real-world: **1.2B Q4_K_M** on a POCO X3 (8 GB) returned short prompts in a few seconds. |
| **“8–12 GB for 3B+”** | **High-side claim** for well-quantized models — do not over-claim in docs or UI. |

Phone path discipline:

- Prefer **short bursts** (one paragraph translate).
- Thermal: pause / cool-down when sustained load is high.
- Default chat quant: **Q4_K_M** (or similar); Opus-MT as specialized MT.

### “Bake into the app”

| What we can ship in APK / repo | What we cannot |
|---|---|
| Runtime (llama.cpp `.so`), JNI, Kotlin glue | Multi‑GB GGUF in git |
| Small Opus-MT pair(s) if under size budget | Full multi-language NLLB without download |
| Download manager for first-run model fetch | Assume Ollama exists on GrapheneOS phone |

Phone path = **download-on-first-use** assets under app files dir + optional Wi‑Fi-only.

## Workspace workflow for the on-device LLM (CLAUDE.md style)

Mirror GrapheneOS workspace layers so prompts stay short and consistent:

```
php-usd-converter/
  llm-workflow/                 # context pack for humans + on-device prompts
    AGENTS.md                   # non-negotiables for translation agent
    CLAUDE.md                   # thin pointer (same idea as Graphene CLAUDE.md)
    CONTEXT.md                  # active languages, model choice, device limits
    REFERENCES.md               # model IDs, quantization, pair list, decisions
    prompts/
      translate.md              # system + user templates
      phonetics.md
    models/
      MANIFEST.md               # which GGUF/ONNX/Opus assets to download
      .gitkeep
  android/ … (later)
    jni/llama.cpp/
    assets/models/              # optional tiny placeholder only
```

**How the LLM uses this:**  
On-device runner loads `prompts/translate.md` + `CONTEXT.md` language table,
not the entire app source. Cloud backends use the same prompt files so desktop
and mobile stay aligned.

## Implementation phases

### Phase 0 — Cloud & free path work (now)

- [x] Multi-backend desktop Translator tab
- [x] Detect xAI **403 no credits** vs bad key vs bad model
- [x] Default cloud models: `grok-4.5` (xAI), `gpt-4o-mini` (OpenAI)
- [x] Free path via **deep-translator** (network, no API key) until on-device MT ships
- [ ] User adds xAI credits → cloud path green (optional)

### Phase 1 — Desktop local MT / GGUF (next)

1. Optional `llama-cpp-python` path for a small GGUF (dev laptop).
2. Opus-MT via `transformers` *or* pre-exported CTranslate2 for en↔es as proof.
3. Wire backend id `local-mt` / `local-gguf` in Settings.
4. Commit each backend separately.

### Phase 2 — Android scaffolding

1. Kotlin Translator surface (tab or companion module).
2. CMake/JNI linking **llama.cpp** (LiteRT spike only if we lock Gemma-3n).
3. Asset download service + checksums from `models/MANIFEST.md`.
4. Thermal: pause / cool-down under sustained load.

### Phase 3 — Pair efficiency

1. Ship only pairs needed (EN↔ES, EN↔FIL/TL, EN↔ZH, …).
2. Opus-MT per pair preferred over one huge chat model.
3. Keep phonics on small chat GGUF or cloud.

### Phase 4 — GrapheneOS packaging

1. Sideload APK; no GMS.
2. Network only for first model download (user consent).
3. Debug export of which model/backend ran (no secrets).

## Commit discipline (rollback-friendly)

| Commit theme | Contents |
|---|---|
| `docs: mobile LLM plan + llm-workflow` | This plan, AGENTS/CLAUDE/CONTEXT pack |
| `fix: xAI errors + model defaults` | backends.py, secrets defaults |
| `feat: offline free translator backend` | deep-translator fallback |
| `feat: wire offline backend in UI` | app.py settings labels |
| Later: `feat(android): jni llama skeleton` | isolated, easy to drop |

**No GitHub Release** until at least one path (free offline *or* cloud with credits)
succeeds end-to-end on a real translate click.

## Success criteria

| Criterion | Pass |
|---|---|
| Desktop: translate EN→ES without API key | Free / offline backend works |
| Desktop: xAI when credits exist | Clear status if 403 credits |
| Prompt pack shared | Same `prompts/translate.md` for all chat backends |
| Mobile plan actionable | Phases 1–3 implementable without redesign |
| Revert safety | Each commit leaves app runnable |

## Open decisions (log in REFERENCES.md when chosen)

- First Opus pairs to ship?
- llama.cpp vs LiteRT for v1 Android? (default **llama.cpp**)
- Max download size for first-run model?
- CTranslate2 vs pure llama.cpp for Marian/Opus weights?
