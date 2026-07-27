"""Local secrets for AI backends — never commit secrets.json."""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any

DEFAULTS: dict[str, Any] = {
    "active_backend": "google",
    "ollama_base_url": "http://127.0.0.1:11434",
    "ollama_model": "tinyllama",
    "xai_api_key": "",
    "xai_model": "grok-4.5",
    "openai_api_key": "",
    "openai_model": "gpt-4o-mini",
    "hf_token": "",
    # Serverless translation model (needs Inference Providers permission on token)
    "hf_model": "google-t5/t5-small",
}


class SecretsStore:
    def __init__(self, path: Path | None = None) -> None:
        root = Path(__file__).resolve().parent.parent
        self.path = path or (root / "secrets.json")
        self._data = dict(DEFAULTS)
        self.load()

    def load(self) -> None:
        if not self.path.is_file():
            return
        try:
            raw = json.loads(self.path.read_text(encoding="utf-8"))
            if isinstance(raw, dict):
                for k, v in DEFAULTS.items():
                    if k in raw:
                        self._data[k] = raw[k]
        except Exception:
            pass

    def save(self) -> None:
        self.path.write_text(
            json.dumps(self._data, indent=2) + "\n", encoding="utf-8"
        )
        try:
            os.chmod(self.path, 0o600)
        except OSError:
            pass

    def get(self, key: str, default: Any = None) -> Any:
        return self._data.get(key, default)

    def set(self, key: str, value: Any) -> None:
        self._data[key] = value

    def update(self, **kwargs: Any) -> None:
        self._data.update(kwargs)

    def as_dict(self) -> dict[str, Any]:
        return dict(self._data)

    @staticmethod
    def mask_key(key: str) -> str:
        if not key:
            return "(not set)"
        if len(key) <= 8:
            return "••••••••"
        return f"••••{key[-4:]}"
