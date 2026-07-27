#!/usr/bin/env bash
# Install a small local model for the Translator tab (Ollama backend).
# Does NOT ship model weights in git — pulls via Ollama after install.
set -euo pipefail

MODEL="${1:-tinyllama}"

if ! command -v ollama >/dev/null 2>&1; then
  echo "Ollama is not installed."
  echo "Install from: https://ollama.com/download"
  echo "Then re-run: $0 $MODEL"
  exit 1
fi

echo "Pulling model: $MODEL"
ollama pull "$MODEL"
echo "Done. In the app Settings / Translator, set backend to Ollama and model to: $MODEL"
ollama list
