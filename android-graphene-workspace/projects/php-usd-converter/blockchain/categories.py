"""Market-view categories for the Blockchain tab (dropdown options)."""

from __future__ import annotations

# id -> (label, short description)
CATEGORIES: dict[str, tuple[str, str]] = {
    "top_volume": (
        "Top 10 volume",
        "Highest 24h volume on Robinhood Chain (all tokens)",
    ),
    "trending_boosts": (
        "Trending · boosts",
        "DexScreener spotlight / paid boosts on Robinhood",
    ),
    "trending_momentum": (
        "Trending · momentum",
        "Biggest |24h %| movers (min volume & liquidity filters)",
    ),
    "memecoins": (
        "Top 10 memecoins",
        "High-volume memes (excludes stables & stock tokens)",
    ),
    "rwa": (
        "RWA / stock tokens",
        "Default equity-style RWAs (NVDA, TSLA, AAPL, GOOGL, MSFT)",
    ),
    "custom": (
        "My contracts",
        "Contracts you added to track",
    ),
}

DEFAULT_CATEGORY = "top_volume"

# Stable order for menus
CATEGORY_ORDER = (
    "top_volume",
    "trending_boosts",
    "trending_momentum",
    "memecoins",
    "rwa",
    "custom",
)


def category_labels() -> list[str]:
    return [CATEGORIES[k][0] for k in CATEGORY_ORDER]


def label_to_id(label: str) -> str:
    for k, (lab, _) in CATEGORIES.items():
        if lab == label:
            return k
    return DEFAULT_CATEGORY


def id_to_label(cat_id: str) -> str:
    return CATEGORIES.get(cat_id, CATEGORIES[DEFAULT_CATEGORY])[0]


def id_to_description(cat_id: str) -> str:
    return CATEGORIES.get(cat_id, CATEGORIES[DEFAULT_CATEGORY])[1]
