"""AI / MT backends for translation + gooble-style phonics."""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Protocol
from urllib import error, request

from .hf_local import translate_seq2seq, transformers_available
from .lang_maps import (
    GOOGLE_CODES,
    HF_API_DEFAULT_MODEL,
    MYMEMORY_NAMES,
    OPUS_EN_MODELS,
    T5_LANGS,
)
from .phonetics_util import free_phonetics
from .secrets_store import SecretsStore

_PROMPTS = Path(__file__).resolve().parent.parent / "llm-workflow" / "prompts"


def _load_prompt_template(name: str, **kwargs: str) -> str:
    path = _PROMPTS / name
    if path.is_file():
        text = path.read_text(encoding="utf-8")
        m = re.search(r"```\n(.*?)```", text, re.S)
        body = m.group(1).strip() if m else text
        try:
            return body.format(**kwargs)
        except Exception:
            pass
    if name.startswith("translate"):
        return (
            f"Translate the following text into {kwargs.get('target_lang', 'English')}.\n"
            "Reply with only the translation, no quotes or commentary.\n\n"
            f"{kwargs.get('text', '').strip()}"
        )
    # gooble-style phonics prompt
    return (
        f"Write out the pronunciation of the following text using a "
        f"{kwargs.get('phonetic_lang', 'English')} phonetic alphabet / romanization.\n"
        "Reply with only the pronunciation.\n\n"
        f"{kwargs.get('text', '').strip()}"
    )


def _translate_prompt(text: str, target_lang: str) -> str:
    return _load_prompt_template("translate.md", target_lang=target_lang, text=text)


def _phonetics_prompt(text: str, phonetic_lang: str) -> str:
    return _load_prompt_template(
        "phonetics.md", phonetic_lang=phonetic_lang, text=text
    )


def _http_json(
    method: str,
    url: str,
    payload: dict | None = None,
    headers: dict | None = None,
    timeout: float = 120.0,
) -> dict | list | str:
    data = None
    hdrs = {"Content-Type": "application/json", **(headers or {})}
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
    req = request.Request(url, data=data, headers=hdrs, method=method)
    try:
        with request.urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8")
            if not body:
                return {}
            return json.loads(body)
    except error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="replace")
        if e.code == 403 and "credits" in err_body.lower():
            raise RuntimeError(
                "xAI team has no credits/licenses yet. "
                "Add billing at https://console.x.ai/ "
                "(your API key is valid, but the account cannot spend). "
                f"Detail: {err_body[:200]}"
            ) from e
        if e.code == 401:
            raise RuntimeError(
                f"Unauthorized (check API key). Server said: {err_body[:200]}"
            ) from e
        raise RuntimeError(f"HTTP {e.code}: {err_body[:500]}") from e
    except error.URLError as e:
        raise RuntimeError(f"Network error: {e.reason}") from e


class TranslationBackend(Protocol):
    name: str

    def available(self) -> tuple[bool, str]: ...

    def translate(self, text: str, target_lang: str) -> str: ...

    def phonetics(self, text: str, phonetic_lang: str) -> str: ...


# ---------------------------------------------------------------------------
# 1) Google free (deep-translator) — no API key
# ---------------------------------------------------------------------------
class GoogleFreeBackend:
    """Network MT via Google free endpoint (deep-translator). No API key."""

    name = "Google free (no API key)"

    def available(self) -> tuple[bool, str]:
        try:
            import deep_translator  # noqa: F401

            return True, "Google free ready (network; no API key)"
        except ImportError:
            return False, "Install: pip install deep-translator"

    def translate(self, text: str, target_lang: str) -> str:
        from deep_translator import GoogleTranslator

        code = GOOGLE_CODES.get(target_lang, "en")
        try:
            return GoogleTranslator(source="auto", target=code).translate(text.strip())
        except Exception as e:
            raise RuntimeError(f"Google free translator failed: {e}") from e

    def phonetics(self, text: str, phonetic_lang: str) -> str:
        return free_phonetics(text, phonetic_lang)


# ---------------------------------------------------------------------------
# 2) MyMemory free — no API key (daily quota)
# ---------------------------------------------------------------------------
class MyMemoryBackend:
    """Network MT via MyMemory free API (deep-translator). No API key."""

    name = "MyMemory free (no API key)"

    def available(self) -> tuple[bool, str]:
        try:
            import deep_translator  # noqa: F401

            return True, "MyMemory free ready (network; daily quota)"
        except ImportError:
            return False, "Install: pip install deep-translator"

    def translate(self, text: str, target_lang: str) -> str:
        from deep_translator import MyMemoryTranslator

        target = MYMEMORY_NAMES.get(target_lang)
        if not target:
            raise RuntimeError(
                f"MyMemory has no mapping for '{target_lang}'. "
                "Try Google free or HF Opus for this language."
            )
        try:
            return MyMemoryTranslator(source="english", target=target).translate(
                text.strip()
            )
        except Exception as e:
            raise RuntimeError(f"MyMemory failed: {e}") from e

    def phonetics(self, text: str, phonetic_lang: str) -> str:
        return free_phonetics(text, phonetic_lang)


# ---------------------------------------------------------------------------
# 3) Hugging Face Opus-MT (local weights from Hub)
# ---------------------------------------------------------------------------
class HFOpusLocalBackend:
    """Dedicated MT: Helsinki-NLP Opus-MT pairs downloaded from Hugging Face Hub.

    Best quality-per-MB for fixed pairs. First use downloads ~200–300MB per pair.
    Assumes English source (gooble-style).
    """

    name = "HF Opus-MT (local from Hub)"

    def available(self) -> tuple[bool, str]:
        ok, msg = transformers_available()
        if not ok:
            return False, msg
        pairs = ", ".join(sorted(OPUS_EN_MODELS.keys())[:6])
        return True, f"Local Opus-MT ready · pairs: {pairs}…"

    def translate(self, text: str, target_lang: str) -> str:
        model_id = OPUS_EN_MODELS.get(target_lang)
        if not model_id:
            supported = ", ".join(sorted(OPUS_EN_MODELS.keys()))
            raise RuntimeError(
                f"No Opus-MT en→{target_lang} pair configured. "
                f"Supported: {supported}. Use Google free / MyMemory for others."
            )
        try:
            return translate_seq2seq(model_id, text.strip())
        except Exception as e:
            raise RuntimeError(f"HF Opus-MT failed ({model_id}): {e}") from e

    def phonetics(self, text: str, phonetic_lang: str) -> str:
        return free_phonetics(text, phonetic_lang)


# ---------------------------------------------------------------------------
# 4) Hugging Face T5-small (local) — limited languages
# ---------------------------------------------------------------------------
class HFT5LocalBackend:
    """google-t5/t5-small from HF Hub. Only EN→German/French/Romanian reliably."""

    name = "HF T5-small (local from Hub)"
    MODEL = "google-t5/t5-small"

    def available(self) -> tuple[bool, str]:
        ok, msg = transformers_available()
        if not ok:
            return False, msg
        return True, "T5-small ready · EN→German/French/Romanian"

    def translate(self, text: str, target_lang: str) -> str:
        t5_name = T5_LANGS.get(target_lang)
        if not t5_name:
            raise RuntimeError(
                f"T5-small only handles German/French/Romanian (got {target_lang}). "
                "Use HF Opus-MT or Google free for Spanish and others."
            )
        prefix = f"translate English to {t5_name}: "
        try:
            return translate_seq2seq(self.MODEL, text.strip(), prefix=prefix)
        except Exception as e:
            raise RuntimeError(f"HF T5-small failed: {e}") from e

    def phonetics(self, text: str, phonetic_lang: str) -> str:
        return free_phonetics(text, phonetic_lang)


# ---------------------------------------------------------------------------
# 5) Hugging Face Inference API (token + router)
# ---------------------------------------------------------------------------
class HuggingFaceAPIBackend:
    """Serverless HF Inference Providers (router). Needs free HF token."""

    name = "HF Inference API (token)"

    def __init__(self, store: SecretsStore) -> None:
        self.store = store

    def _token(self) -> str:
        return str(self.store.get("hf_token", "") or "").strip()

    def _model(self) -> str:
        return str(
            self.store.get("hf_model", "") or HF_API_DEFAULT_MODEL
        ).strip() or HF_API_DEFAULT_MODEL

    def available(self) -> tuple[bool, str]:
        if not self._token():
            return (
                False,
                "HF token not set — create free token at "
                "https://huggingface.co/settings/tokens "
                "(permission: Inference Providers)",
            )
        return True, f"HF API · model {self._model()}"

    def _infer(self, payload: dict) -> str:
        token = self._token()
        if not token:
            raise RuntimeError("Hugging Face token not set. Add it in Settings.")
        model = self._model()
        # 2026: router.huggingface.co (legacy api-inference.huggingface.co often dead)
        url = f"https://router.huggingface.co/hf-inference/models/{model}"
        out = _http_json(
            "POST",
            url,
            payload=payload,
            headers={"Authorization": f"Bearer {token}"},
            timeout=90.0,
        )
        if isinstance(out, list) and out:
            item = out[0]
            if isinstance(item, dict):
                return str(
                    item.get("translation_text")
                    or item.get("generated_text")
                    or item
                ).strip()
            return str(item).strip()
        if isinstance(out, dict):
            if "error" in out:
                raise RuntimeError(str(out["error"]))
            return str(
                out.get("translation_text") or out.get("generated_text") or out
            ).strip()
        return str(out).strip()

    def translate(self, text: str, target_lang: str) -> str:
        model = self._model().lower()
        text = text.strip()
        # T5-style models need a task prefix
        if "t5" in model:
            t5_name = T5_LANGS.get(target_lang, target_lang)
            payload = {
                "inputs": f"translate English to {t5_name}: {text}",
            }
        else:
            payload = {"inputs": text}
        return self._infer(payload)

    def phonetics(self, text: str, phonetic_lang: str) -> str:
        # chatty models can do phonics; pure MT models fall back to free IPA
        model = self._model().lower()
        if any(x in model for x in ("instruct", "chat", "llama", "mistral", "gemma", "phi")):
            prompt = _phonetics_prompt(text, phonetic_lang)
            return self._infer(
                {
                    "inputs": prompt,
                    "parameters": {
                        "max_new_tokens": 256,
                        "temperature": 0.2,
                        "return_full_text": False,
                    },
                }
            )
        return free_phonetics(text, phonetic_lang)


# ---------------------------------------------------------------------------
# Ollama / cloud LLMs
# ---------------------------------------------------------------------------
class OllamaBackend:
    name = "Ollama (local LLM)"

    def __init__(self, store: SecretsStore) -> None:
        self.store = store

    def _base(self) -> str:
        return str(self.store.get("ollama_base_url", "http://127.0.0.1:11434")).rstrip(
            "/"
        )

    def _model(self) -> str:
        return str(self.store.get("ollama_model", "tinyllama"))

    def available(self) -> tuple[bool, str]:
        try:
            tags = _http_json("GET", f"{self._base()}/api/tags", timeout=3.0)
            models = []
            if isinstance(tags, dict):
                models = [m.get("name", "") for m in tags.get("models", [])]
            model = self._model()
            if model in models or any(model.split(":")[0] in n for n in models):
                return True, f"Ollama OK · {model} · {len(models)} model(s)"
            if models:
                return (
                    True,
                    f"Ollama up; '{model}' missing. Have: {', '.join(models[:5])}",
                )
            return True, "Ollama running (pull a model: ./scripts/setup_ollama_tiny.sh)"
        except Exception as e:
            return False, f"Ollama unavailable: {e}"

    def _chat(self, prompt: str) -> str:
        payload = {
            "model": self._model(),
            "messages": [
                {
                    "role": "system",
                    "content": "You are a translation-only assistant. Output only the requested text.",
                },
                {"role": "user", "content": prompt},
            ],
            "stream": False,
            "options": {"temperature": 0.2},
        }
        out = _http_json("POST", f"{self._base()}/api/chat", payload=payload)
        if isinstance(out, dict):
            msg = out.get("message") or {}
            content = msg.get("content") or out.get("response") or ""
            return str(content).strip()
        return str(out).strip()

    def translate(self, text: str, target_lang: str) -> str:
        return self._chat(_translate_prompt(text, target_lang))

    def phonetics(self, text: str, phonetic_lang: str) -> str:
        return self._chat(_phonetics_prompt(text, phonetic_lang))


class OpenAICompatibleBackend:
    def __init__(
        self,
        store: SecretsStore,
        *,
        name: str,
        key_field: str,
        model_field: str,
        base_url: str,
        default_model: str,
    ) -> None:
        self.store = store
        self.name = name
        self.key_field = key_field
        self.model_field = model_field
        self.base_url = base_url.rstrip("/")
        self.default_model = default_model

    def _key(self) -> str:
        return str(self.store.get(self.key_field, "") or "").strip()

    def _model(self) -> str:
        return str(self.store.get(self.model_field, "") or self.default_model).strip()

    def available(self) -> tuple[bool, str]:
        if not self._key():
            return False, f"{self.name}: API key not set (Settings)"
        return True, f"{self.name} · model {self._model()} (needs account credits)"

    def _chat(self, prompt: str) -> str:
        key = self._key()
        if not key:
            raise RuntimeError(f"{self.name} API key not set. Add it in Settings.")
        model = self._model()
        payload = {
            "model": model,
            "messages": [
                {
                    "role": "system",
                    "content": "You are a translation-only assistant. Output only the requested text.",
                },
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.2,
        }
        url = f"{self.base_url}/chat/completions"
        out = _http_json(
            "POST",
            url,
            payload=payload,
            headers={"Authorization": f"Bearer {key}"},
        )
        if isinstance(out, dict):
            choices = out.get("choices") or []
            if choices:
                msg = choices[0].get("message") or {}
                content = msg.get("content") or choices[0].get("text") or ""
                return str(content).strip()
            if out.get("output_text"):
                return str(out["output_text"]).strip()
        raise RuntimeError(f"{self.name}: unexpected response: {str(out)[:300]}")

    def translate(self, text: str, target_lang: str) -> str:
        return self._chat(_translate_prompt(text, target_lang))

    def phonetics(self, text: str, phonetic_lang: str) -> str:
        return self._chat(_phonetics_prompt(text, phonetic_lang))


# ---------------------------------------------------------------------------
# Registry
# ---------------------------------------------------------------------------
# Primary working set first; cloud/local-LLM after.
BACKEND_IDS = (
    "google",
    "mymemory",
    "hf_opus",
    "hf_t5",
    "huggingface",
    "ollama",
    "xai",
    "openai",
)

BACKEND_LABELS = {
    "google": "Google free (no API key)",
    "mymemory": "MyMemory free (no API key)",
    "hf_opus": "HF Opus-MT (local Hub)",
    "hf_t5": "HF T5-small (local Hub)",
    "huggingface": "HF Inference API (token)",
    "ollama": "Ollama (local LLM)",
    "xai": "xAI / Grok (API key)",
    "openai": "OpenAI (API key)",
    # aliases kept for old secrets.json
    "offline": "Google free (no API key)",
    "free": "Google free (no API key)",
}


def list_backends() -> list[tuple[str, str]]:
    return [(bid, BACKEND_LABELS[bid]) for bid in BACKEND_IDS]


def get_backend(store: SecretsStore, backend_id: str | None = None) -> TranslationBackend:
    bid = (backend_id or store.get("active_backend") or "google").lower()
    if bid in ("google", "offline", "free", "deep"):
        return GoogleFreeBackend()
    if bid in ("mymemory", "my_memory", "memory"):
        return MyMemoryBackend()
    if bid in ("hf_opus", "opus", "opus_mt", "hf-opus"):
        return HFOpusLocalBackend()
    if bid in ("hf_t5", "t5", "hf-t5"):
        return HFT5LocalBackend()
    if bid in ("huggingface", "hf", "hf_api"):
        return HuggingFaceAPIBackend(store)
    if bid == "ollama":
        return OllamaBackend(store)
    if bid == "xai":
        return OpenAICompatibleBackend(
            store,
            name="xAI",
            key_field="xai_api_key",
            model_field="xai_model",
            base_url="https://api.x.ai/v1",
            default_model="grok-4.5",
        )
    if bid == "openai":
        return OpenAICompatibleBackend(
            store,
            name="OpenAI",
            key_field="openai_api_key",
            model_field="openai_model",
            base_url="https://api.openai.com/v1",
            default_model="gpt-4o-mini",
        )
    raise ValueError(f"Unknown backend: {bid}")
