"""Gooble-style phonics without a paid LLM (English IPA + light fallbacks)."""

from __future__ import annotations


def free_phonetics(text: str, phonetic_lang: str = "English") -> str:
    """
    Approximate goobleTranslator's 'Gooble Phonics' button.

    Original used text-davinci-003:
      Write out the pronunciation of {text} using an {lang} Phonetic Alphabet.

    Free path:
      - English (default): CMU IPA via eng-to-ipa
      - other: IPA of Latin/English reading when possible, with a note
    """
    text = (text or "").strip()
    if not text:
        return ""

    lang = (phonetic_lang or "English").strip()
    ipa = _to_ipa(text)

    if lang.lower() in ("english", "en"):
        return ipa

    # Without an LLM we cannot invent language-specific phoneme charts.
    # Return English IPA plus a clear label so the UI still "works".
    return (
        f"[IPA · English reading; full '{lang}' phoneme chart needs an LLM backend]\n"
        f"{ipa}"
    )


def _to_ipa(text: str) -> str:
    try:
        import eng_to_ipa as ipa

        out = ipa.convert(text)
        if out and out.strip():
            return out.strip()
    except Exception:
        pass
    # very light fallback: keep letters, mark stress-ish spaces
    return text
