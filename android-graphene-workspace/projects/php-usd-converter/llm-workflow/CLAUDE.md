# CLAUDE.md — pointer for translation LLM workflow

**Read `AGENTS.md` first.** It is the source of truth for translation behavior.

Then:

1. `CONTEXT.md` — active languages, chosen on-device model, device limits  
2. `prompts/translate.md` / `prompts/phonetics.md` — exact templates  
3. `REFERENCES.md` — model IDs, quantization, decisions  

This folder exists so **desktop cloud**, **Ollama**, and **future Android
llama.cpp / Opus-MT** runners all share one context pack — same idea as the
GrapheneOS workspace `CLAUDE.md` → `AGENTS.md` pattern.
