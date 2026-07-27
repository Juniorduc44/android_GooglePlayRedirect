"""AI backends for translation (Ollama, xAI, OpenAI, Hugging Face, offline)."""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Protocol
from urllib import error, request

from .secrets_store import SecretsStore

_PROMPTS = Path(__file__).resolve().parent.parent / "llm-workflow" / "prompts"


def _load_prompt_template(name: str, **kwargs: str) -> str:
    """Load prompt file and substitute {vars}; fallback to built-in."""
    path = _PROMPTS / name
    if path.is_file():
        text = path.read_text(encoding="utf-8")
        # use the fenced user block if present
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
    return (
        f"Write the pronunciation of the following text using a "
        f"{kwargs.get('phonetic_lang', 'English')} phonetic alphabet / romanization.\n"
        "Reply with only the pronunciation.\n\n"
        f"{kwargs.get('text', '').strip()}"
    )


def _translate_prompt(text: str, target_lang: str) -> str:
    return _load_prompt_template(
        "translate.md", target_lang=target_lang, text=text
    )


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
        # Friendly xAI billing / model messages
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


class OfflineFreeBackend:
    """Works without API keys using deep-translator (Google translate free endpoint).

    Not on-device ML — network required — but unblocks Translator until Opus-MT/GGUF ships.
    """

    name = "Offline free (deep-translator)"

    # map display names → deep_translator codes
    LANG_CODES = {
        "Amharic": "am",
        "Arabic": "ar",
        "Bengali": "bn",
        "English": "en",
        "French": "fr",
        "German": "de",
        "Gujarati": "gu",
        "Hausa": "ha",
        "Hindi": "hi",
        "Igbo": "ig",
        "Japanese": "ja",
        "Javanese": "jw",
        "Kannada": "kn",
        "Korean": "ko",
        "Malay": "ms",
        "Malayalam": "ml",
        "Mandarin Chinese": "zh-CN",
        "Marathi": "mr",
        "Polish": "pl",
        "Portuguese": "pt",
        "Punjabi": "pa",
        "Russian": "ru",
        "Somali": "so",
        "Spanish": "es",
        "Swahili": "sw",
        "Tamil": "ta",
        "Telugu": "te",
        "Turkish": "tr",
        "Urdu": "ur",
        "Vietnamese": "vi",
        "Yoruba": "yo",
        "Zulu": "zu",
        # approximations / unsupported fall back to en
        "Fulani": "ff",
        "Shona": "sn",
        "Tigrinya": "ti",
        "Wu Chinese": "zh-CN",
    }

    def available(self) -> tuple[bool, str]:
        try:
            import deep_translator  # noqa: F401

            return True, "Free translator ready (network; no API key)"
        except ImportError:
            return (
                False,
                "Install deep-translator: pip install deep-translator",
            )

    def _code(self, lang_name: str) -> str:
        return self.LANG_CODES.get(lang_name, "en")

    def translate(self, text: str, target_lang: str) -> str:
        from deep_translator import GoogleTranslator

        code = self._code(target_lang)
        try:
            return GoogleTranslator(source="auto", target=code).translate(text.strip())
        except Exception as e:
            raise RuntimeError(f"Free translator failed: {e}") from e

    def phonetics(self, text: str, phonetic_lang: str) -> str:
        # Free path: return romanization hint via translate-to-latin if possible
        # deep-translator has no phonetics — explain clearly
        raise RuntimeError(
            "Phonics needs an LLM backend (Ollama / xAI / OpenAI / HF). "
            "Free offline path only does plain translation."
        )


class OllamaBackend:
    name = "Ollama (local)"

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


class HuggingFaceBackend:
    name = "Hugging Face"

    def __init__(self, store: SecretsStore) -> None:
        self.store = store

    def _token(self) -> str:
        return str(self.store.get("hf_token", "") or "").strip()

    def _model(self) -> str:
        return str(
            self.store.get("hf_model", "meta-llama/Llama-3.2-3B-Instruct") or ""
        ).strip()

    def available(self) -> tuple[bool, str]:
        if not self._token():
            return False, "Hugging Face: token not set"
        return True, f"Hugging Face · {self._model()}"

    def _generate(self, prompt: str) -> str:
        token = self._token()
        if not token:
            raise RuntimeError("Hugging Face token not set. Add it in Settings.")
        model = self._model()
        url = f"https://api-inference.huggingface.co/models/{model}"
        payload = {
            "inputs": prompt,
            "parameters": {
                "max_new_tokens": 512,
                "temperature": 0.2,
                "return_full_text": False,
            },
        }
        out = _http_json(
            "POST",
            url,
            payload=payload,
            headers={"Authorization": f"Bearer {token}"},
        )
        if isinstance(out, list) and out:
            item = out[0]
            if isinstance(item, dict):
                return str(
                    item.get("generated_text") or item.get("translation_text") or item
                ).strip()
            return str(item).strip()
        if isinstance(out, dict):
            if "error" in out:
                raise RuntimeError(str(out["error"]))
            return str(out.get("generated_text") or out).strip()
        return str(out).strip()

    def translate(self, text: str, target_lang: str) -> str:
        return self._generate(_translate_prompt(text, target_lang))

    def phonetics(self, text: str, phonetic_lang: str) -> str:
        return self._generate(_phonetics_prompt(text, phonetic_lang))


BACKEND_IDS = ("offline", "ollama", "xai", "openai", "huggingface")

BACKEND_LABELS = {
    "offline": "Free offline (no API key)",
    "ollama": "Ollama (local LLM)",
    "xai": "xAI / Grok (API key)",
    "openai": "OpenAI (API key)",
    "huggingface": "Hugging Face (token)",
}


def list_backends() -> list[tuple[str, str]]:
    return [(bid, BACKEND_LABELS[bid]) for bid in BACKEND_IDS]


def get_backend(store: SecretsStore, backend_id: str | None = None) -> TranslationBackend:
    bid = (backend_id or store.get("active_backend") or "offline").lower()
    if bid in ("offline", "free", "deep"):
        return OfflineFreeBackend()
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
    if bid in ("huggingface", "hf"):
        return HuggingFaceBackend(store)
    raise ValueError(f"Unknown backend: {bid}")
