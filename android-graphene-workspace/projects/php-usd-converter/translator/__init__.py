"""Translator package — multi-backend AI translation (from goobleTranslator)."""

from .backends import get_backend, list_backends
from .languages import LANGUAGES
from .secrets_store import SecretsStore

__all__ = ["LANGUAGES", "SecretsStore", "get_backend", "list_backends"]
