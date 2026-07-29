"""Default RWA / stock-token contracts on Robinhood Chain (DexScreener-verified).

Addresses verified live against api.dexscreener.com (chainId=robinhood) on 2026-07-29.
Stock Tokens provide economic exposure only — see Robinhood disclosures.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class DefaultAsset:
    symbol: str
    name: str
    address: str
    category: str  # "rwa" | "stable" | "meme"
    note: str = ""


# Five primary RWA / equity stock-token style assets (liquid Uniswap pairs)
DEFAULT_RWA: tuple[DefaultAsset, ...] = (
    DefaultAsset(
        "NVDA",
        "NVIDIA Stock Token",
        "0xd0601CE157Db5bdC3162BbaC2a2C8aF5320D9EEC",
        "rwa",
        "Equity RWA-style token on Robinhood Chain",
    ),
    DefaultAsset(
        "TSLA",
        "Tesla Stock Token",
        "0x322F0929c4625eD5bAd873c95208D54E1c003b2d",
        "rwa",
    ),
    DefaultAsset(
        "AAPL",
        "Apple Stock Token",
        "0xaF3D76f1834A1d425780943C99Ea8A608f8a93f9",
        "rwa",
    ),
    DefaultAsset(
        "GOOGL",
        "Alphabet Stock Token",
        "0x2e0847E8910a9732eB3fb1bb4b70a580ADAD4FE3",
        "rwa",
    ),
    DefaultAsset(
        "MSFT",
        "Microsoft Stock Token",
        "0xe93237C50D904957Cf27E7B1133b510C669c2e74",
        "rwa",
    ),
)

# Symbols excluded from “memecoin” top-10 ranking
MEME_EXCLUDE_SYMBOLS = frozenset(
    {
        "USDG",
        "WETH",
        "ETH",
        "USDC",
        "USDT",
        "DAI",
        "NVDA",
        "TSLA",
        "AAPL",
        "GOOGL",
        "MSFT",
        "AMZN",
        "META",
        "HOOD",  # ticker collision with exchange — still may appear as meme; keep filter soft
    }
)

# Seed search queries to discover liquid robinhood pairs
MEME_SEED_QUERIES = (
    "robinhood",
    "meme",
    "pepe",
    "doge",
    "cat",
    "hood",
    "mars",
    "space",
    "frog",
    "inu",
    "ai",
    "coin",
    "token",
    "elon",
)
