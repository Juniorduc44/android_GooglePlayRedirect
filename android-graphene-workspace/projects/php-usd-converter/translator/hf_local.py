"""Load Marian/T5 translation models from the Hugging Face Hub (local inference)."""

from __future__ import annotations

from functools import lru_cache
from typing import Any

_CACHE: dict[str, tuple[Any, Any]] = {}


def transformers_available() -> tuple[bool, str]:
    try:
        import torch
        import transformers  # noqa: F401

        return True, f"transformers OK · torch {torch.__version__}"
    except ImportError:
        return (
            False,
            "Install local HF stack: pip install transformers sentencepiece sacremoses torch",
        )


def _load_seq2seq(model_id: str):
    if model_id in _CACHE:
        return _CACHE[model_id]
    from transformers import AutoModelForSeq2SeqLM, AutoTokenizer

    tok = AutoTokenizer.from_pretrained(model_id)
    mod = AutoModelForSeq2SeqLM.from_pretrained(model_id)
    mod.eval()
    _CACHE[model_id] = (tok, mod)
    return tok, mod


def translate_seq2seq(
    model_id: str,
    text: str,
    *,
    max_new_tokens: int = 256,
    prefix: str | None = None,
) -> str:
    """Run a local seq2seq model. Optional T5-style task prefix."""
    import torch

    tok, mod = _load_seq2seq(model_id)
    src = f"{prefix}{text}" if prefix else text
    inputs = tok(src, return_tensors="pt", truncation=True, max_length=512)
    with torch.no_grad():
        out = mod.generate(**inputs, max_new_tokens=max_new_tokens)
    return tok.decode(out[0], skip_special_tokens=True).strip()


def clear_cache() -> None:
    _CACHE.clear()


@lru_cache(maxsize=1)
def list_cached_models() -> tuple[str, ...]:
    return tuple(sorted(_CACHE.keys()))
