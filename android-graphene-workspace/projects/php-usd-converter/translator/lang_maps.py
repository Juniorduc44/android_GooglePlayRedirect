"""Language name maps for free MT engines and Hugging Face Opus-MT pairs."""

from __future__ import annotations

# Display name (gooble list + Filipino) → Google / deep-translator ISO codes
# Note: Google labels Filipino as code "tl" (Tagalog family).
GOOGLE_CODES: dict[str, str] = {
    "Amharic": "am",
    "Arabic": "ar",
    "Bengali": "bn",
    "English": "en",
    "Filipino": "tl",
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
    "Tagalog": "tl",
    "Tamil": "ta",
    "Telugu": "te",
    "Turkish": "tr",
    "Urdu": "ur",
    "Vietnamese": "vi",
    "Yoruba": "yo",
    "Zulu": "zu",
    "Fulani": "ff",
    "Shona": "sn",
    "Tigrinya": "ti",
    "Wu Chinese": "zh-CN",
}

# MyMemory wants language *names* (lowercase) for many pairs
MYMEMORY_NAMES: dict[str, str] = {
    "Amharic": "amharic",
    "Arabic": "arabic",
    "Bengali": "bengali",
    "English": "english",
    "Filipino": "filipino",
    "French": "french",
    "German": "german",
    "Gujarati": "gujarati",
    "Hausa": "hausa",
    "Hindi": "hindi",
    "Igbo": "igbo",
    "Japanese": "japanese",
    "Javanese": "javanese",
    "Kannada": "kannada",
    "Korean": "korean",
    "Malay": "malay",
    "Malayalam": "malayalam",
    "Mandarin Chinese": "chinese (simplified)",
    "Marathi": "marathi",
    "Polish": "polish",
    "Portuguese": "portuguese",
    "Punjabi": "punjabi",
    "Russian": "russian",
    "Somali": "somali",
    "Spanish": "spanish",
    "Swahili": "swahili",
    "Tagalog": "tagalog",
    "Tamil": "tamil",
    "Telugu": "telugu",
    "Turkish": "turkish",
    "Urdu": "urdu",
    "Vietnamese": "vietnamese",
    "Yoruba": "yoruba",
    "Zulu": "zulu",
    "Shona": "shona",
    "Wu Chinese": "chinese (simplified)",
}

# English → target Helsinki-NLP Opus-MT model IDs (loaded from HF Hub)
# Source is assumed English (gooble-style). First-run downloads ~300MB per pair.
OPUS_EN_MODELS: dict[str, str] = {
    "Arabic": "Helsinki-NLP/opus-mt-en-ar",
    "Filipino": "Helsinki-NLP/opus-mt-en-tl",
    "French": "Helsinki-NLP/opus-mt-en-fr",
    "German": "Helsinki-NLP/opus-mt-en-de",
    "Hindi": "Helsinki-NLP/opus-mt-en-hi",
    "Japanese": "Helsinki-NLP/opus-mt-en-jap",
    "Korean": "Helsinki-NLP/opus-mt-en-ko",
    "Mandarin Chinese": "Helsinki-NLP/opus-mt-en-zh",
    "Polish": "Helsinki-NLP/opus-mt-en-pl",
    "Portuguese": "Helsinki-NLP/opus-mt-en-ROMANCE",
    "Russian": "Helsinki-NLP/opus-mt-en-ru",
    "Spanish": "Helsinki-NLP/opus-mt-en-es",
    "Tagalog": "Helsinki-NLP/opus-mt-en-tl",
    "Turkish": "Helsinki-NLP/opus-mt-en-tr",
    "Vietnamese": "Helsinki-NLP/opus-mt-en-vi",
    "Wu Chinese": "Helsinki-NLP/opus-mt-en-zh",
}

# T5-small only knows these (prefix: "translate English to X:")
T5_LANGS: dict[str, str] = {
    "German": "German",
    "French": "French",
    "Romanian": "Romanian",  # not in gooble list but valid for T5
}

# Default HF Inference API translation model (needs token + router)
HF_API_DEFAULT_MODEL = "google-t5/t5-small"
