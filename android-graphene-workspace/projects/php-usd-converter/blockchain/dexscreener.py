"""DexScreener HTTP client with defensive try/except and timeouts."""

from __future__ import annotations

import json
import re
import time
from dataclasses import dataclass, field
from typing import Any
from urllib import error, parse, request

USER_AGENT = "php-usd-converter-blockchain/1.6 (+local; research)"
BASE = "https://api.dexscreener.com"
_ADDRESS_RE = re.compile(r"^0x[a-fA-F0-9]{40}$")


class DexScreenerError(RuntimeError):
    """User-facing / loggable API failure."""


@dataclass
class PairQuote:
    chain_id: str
    dex_id: str
    url: str
    pair_address: str
    base_symbol: str
    base_name: str
    base_address: str
    quote_symbol: str
    price_usd: float | None
    price_native: str | None
    volume_h24: float
    liquidity_usd: float
    price_change_h24: float | None
    market_cap: float | None
    fdv: float | None
    raw: dict[str, Any] = field(repr=False, default_factory=dict)

    def short_address(self, n: int = 6) -> str:
        a = self.base_address or ""
        if len(a) < 12:
            return a
        return f"{a[: n + 2]}…{a[-n:]}"


def is_valid_evm_address(addr: str) -> bool:
    try:
        return bool(_ADDRESS_RE.match((addr or "").strip()))
    except Exception:
        return False


def _http_get_json(url: str, timeout: float = 25.0) -> Any:
    req = request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/json",
        },
        method="GET",
    )
    try:
        with request.urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8", errors="replace")
            if not body:
                return {}
            try:
                return json.loads(body)
            except json.JSONDecodeError as e:
                raise DexScreenerError(f"Invalid JSON from DexScreener: {e}") from e
    except error.HTTPError as e:
        detail = ""
        try:
            detail = e.read().decode("utf-8", errors="replace")[:200]
        except Exception:
            pass
        raise DexScreenerError(f"HTTP {e.code} from DexScreener: {detail or e.reason}") from e
    except error.URLError as e:
        raise DexScreenerError(f"Network error talking to DexScreener: {e.reason}") from e
    except TimeoutError as e:
        raise DexScreenerError("DexScreener request timed out") from e
    except Exception as e:
        raise DexScreenerError(f"Unexpected DexScreener failure: {type(e).__name__}: {e}") from e


def _num(x: Any) -> float | None:
    if x is None or x == "":
        return None
    try:
        return float(x)
    except (TypeError, ValueError):
        return None


def pair_from_dict(p: dict[str, Any]) -> PairQuote | None:
    try:
        if not isinstance(p, dict):
            return None
        bt = p.get("baseToken") or {}
        qt = p.get("quoteToken") or {}
        vol = p.get("volume") or {}
        liq = p.get("liquidity") or {}
        chg = p.get("priceChange") or {}
        return PairQuote(
            chain_id=str(p.get("chainId") or ""),
            dex_id=str(p.get("dexId") or ""),
            url=str(p.get("url") or ""),
            pair_address=str(p.get("pairAddress") or ""),
            base_symbol=str(bt.get("symbol") or "?"),
            base_name=str(bt.get("name") or ""),
            base_address=str(bt.get("address") or ""),
            quote_symbol=str(qt.get("symbol") or ""),
            price_usd=_num(p.get("priceUsd")),
            price_native=str(p.get("priceNative")) if p.get("priceNative") is not None else None,
            volume_h24=float(_num(vol.get("h24")) or 0.0),
            liquidity_usd=float(_num(liq.get("usd")) or 0.0),
            price_change_h24=_num(chg.get("h24")),
            market_cap=_num(p.get("marketCap")),
            fdv=_num(p.get("fdv")),
            raw=p,
        )
    except Exception:
        return None


def search_pairs(query: str, *, chain: str | None = "robinhood", timeout: float = 25.0) -> list[PairQuote]:
    q = (query or "").strip()
    if not q:
        return []
    url = f"{BASE}/latest/dex/search?q={parse.quote(q)}"
    data = _http_get_json(url, timeout=timeout)
    pairs = data.get("pairs") if isinstance(data, dict) else None
    if not isinstance(pairs, list):
        return []
    out: list[PairQuote] = []
    for p in pairs:
        pq = pair_from_dict(p)
        if not pq:
            continue
        if chain and pq.chain_id.lower() != chain.lower():
            continue
        out.append(pq)
    return out


def tokens_by_address(
    addresses: list[str],
    *,
    chain: str = "robinhood",
    timeout: float = 25.0,
) -> list[PairQuote]:
    """Fetch pairs for up to ~30 token addresses (comma-separated)."""
    cleaned: list[str] = []
    for a in addresses:
        a = (a or "").strip()
        if is_valid_evm_address(a):
            cleaned.append(a)
    if not cleaned:
        return []
    # DexScreener allows multiple addresses comma-separated
    joined = ",".join(cleaned)
    url = f"{BASE}/tokens/v1/{parse.quote(chain)}/{joined}"
    data = _http_get_json(url, timeout=timeout)
    if not isinstance(data, list):
        return []
    out: list[PairQuote] = []
    for p in data:
        pq = pair_from_dict(p)
        if pq:
            out.append(pq)
    return out


def best_pair_per_token(pairs: list[PairQuote]) -> dict[str, PairQuote]:
    """Pick highest-liquidity pair per base token address (lowercased)."""
    best: dict[str, PairQuote] = {}
    for p in pairs:
        key = (p.base_address or "").lower()
        if not key:
            continue
        prev = best.get(key)
        if prev is None or p.liquidity_usd > prev.liquidity_usd:
            best[key] = p
    return best


def sleep_politely(seconds: float = 0.15) -> None:
    try:
        time.sleep(max(0.0, seconds))
    except Exception:
        pass
