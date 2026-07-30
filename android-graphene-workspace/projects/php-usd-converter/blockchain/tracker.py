"""Orchestrate RWA defaults + top memecoins + custom contracts."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from .categories import DEFAULT_CATEGORY
from .defaults import DEFAULT_RWA, MEME_EXCLUDE_SYMBOLS, MEME_SEED_QUERIES
from .dexscreener import (
    DexScreenerError,
    PairQuote,
    best_pair_per_token,
    is_valid_evm_address,
    search_pairs,
    sleep_politely,
    token_boosts,
    tokens_by_address,
)
from .networks import DEFAULT_NETWORK_ID, Network, get_network

# Lean seed set — full list was too slow (many sequential DexScreener calls)
_VOLUME_SEED_QUERIES = (
    "robinhood",
    "USDG",
    "NVDA",
    "TSLA",
    "AAPL",
    "meme",
    "pepe",
    "doge",
    "cat",
    "ai",
)
# In-process cache so switching categories / re-opening isn't glacial
_PAIR_CACHE: dict[str, tuple[float, list]] = {}
_PAIR_CACHE_TTL_SEC = 90.0


@dataclass
class TrackedAsset:
    symbol: str
    name: str
    address: str
    category: str
    price_usd: float | None = None
    change_h24: float | None = None
    volume_h24: float = 0.0
    liquidity_usd: float = 0.0
    market_cap: float | None = None
    fdv: float | None = None
    estimated_supply: float | None = None
    pair_address: str = ""
    dex_id: str = ""
    url: str = ""
    error: str | None = None
    note: str = ""

    def format_price(self) -> str:
        if self.error:
            return "—"
        if self.price_usd is None:
            return "n/a"
        p = self.price_usd
        if p >= 100:
            return f"${p:,.2f}"
        if p >= 1:
            return f"${p:,.4f}"
        if p >= 0.0001:
            return f"${p:.6f}"
        return f"${p:.8f}"

    def format_change(self) -> str:
        if self.change_h24 is None:
            return "—"
        sign = "+" if self.change_h24 >= 0 else ""
        return f"{sign}{self.change_h24:.2f}%"

    def short_contract(self) -> str:
        a = self.address or ""
        if len(a) < 12:
            return a
        return f"{a[:8]}…{a[-6:]}"

    def as_dict(self) -> dict[str, Any]:
        return {
            "symbol": self.symbol,
            "name": self.name,
            "address": self.address,
            "category": self.category,
            "price_usd": self.price_usd,
            "change_h24": self.change_h24,
            "volume_h24": self.volume_h24,
            "liquidity_usd": self.liquidity_usd,
            "market_cap": self.market_cap,
            "fdv": self.fdv,
            "estimated_supply": self.estimated_supply,
            "pair_address": self.pair_address,
            "dex_id": self.dex_id,
            "url": self.url,
            "error": self.error,
            "note": self.note,
        }


def _estimate_supply(
    price: float | None, market_cap: float | None, fdv: float | None
) -> float | None:
    if price is None or price <= 0:
        return None
    for cap in (market_cap, fdv):
        if cap is not None and cap > 0:
            return cap / price
    return None


def _from_pair(p: PairQuote, category: str, note: str = "") -> TrackedAsset:
    return TrackedAsset(
        symbol=p.base_symbol,
        name=p.base_name,
        address=p.base_address,
        category=category,
        price_usd=p.price_usd,
        change_h24=p.price_change_h24,
        volume_h24=p.volume_h24,
        liquidity_usd=p.liquidity_usd,
        market_cap=p.market_cap,
        fdv=p.fdv,
        estimated_supply=_estimate_supply(p.price_usd, p.market_cap, p.fdv),
        pair_address=p.pair_address,
        dex_id=p.dex_id,
        url=p.url,
        note=note,
    )


class PriceTracker:
    def __init__(self, network_id: str | None = None) -> None:
        self.network_id = network_id or DEFAULT_NETWORK_ID
        self._custom_addresses: list[str] = []

    @property
    def network(self) -> Network:
        return get_network(self.network_id)

    def set_network(self, network_id: str) -> None:
        # validate
        get_network(network_id)
        self.network_id = network_id

    def add_custom_contract(self, address: str) -> str:
        """Validate and add; returns normalized address or raises ValueError."""
        addr = (address or "").strip()
        if not is_valid_evm_address(addr):
            raise ValueError("Contract must be a 0x… 40-hex EVM address")
        # checksum-insensitive store
        low = addr.lower()
        if low not in {a.lower() for a in self._custom_addresses}:
            self._custom_addresses.append(addr)
        return addr

    def clear_custom(self) -> None:
        self._custom_addresses.clear()

    def list_custom(self) -> list[str]:
        return list(self._custom_addresses)

    def fetch_rwa(self) -> list[TrackedAsset]:
        """Fetch the 5 default RWA/stock tokens. Never raises — per-asset errors."""
        net = self.network
        addrs = [a.address for a in DEFAULT_RWA]
        meta = {a.address.lower(): a for a in DEFAULT_RWA}
        results: list[TrackedAsset] = []
        try:
            pairs = tokens_by_address(addrs, chain=net.dexscreener_slug)
            best = best_pair_per_token(pairs)
        except DexScreenerError as e:
            # total failure — mark all
            for a in DEFAULT_RWA:
                results.append(
                    TrackedAsset(
                        symbol=a.symbol,
                        name=a.name,
                        address=a.address,
                        category="rwa",
                        error=str(e),
                        note=a.note,
                    )
                )
            return results
        except Exception as e:
            for a in DEFAULT_RWA:
                results.append(
                    TrackedAsset(
                        symbol=a.symbol,
                        name=a.name,
                        address=a.address,
                        category="rwa",
                        error=f"{type(e).__name__}: {e}",
                        note=a.note,
                    )
                )
            return results

        for a in DEFAULT_RWA:
            pq = best.get(a.address.lower())
            if pq is None:
                results.append(
                    TrackedAsset(
                        symbol=a.symbol,
                        name=a.name,
                        address=a.address,
                        category="rwa",
                        error="No DexScreener pair found",
                        note=a.note,
                    )
                )
            else:
                t = _from_pair(pq, "rwa", note=a.note)
                # prefer curated display names
                t.symbol = a.symbol
                t.name = a.name
                results.append(t)
        return results

    def fetch_top_memecoins(self, limit: int = 10) -> list[TrackedAsset]:
        """Top memecoins by 24h volume on the selected chain (DexScreener discovery)."""
        net = self.network
        all_pairs: list[PairQuote] = []
        errors: list[str] = []
        for q in MEME_SEED_QUERIES:
            try:
                all_pairs.extend(search_pairs(q, chain=net.dexscreener_slug))
                sleep_politely(0.12)
            except DexScreenerError as e:
                errors.append(f"{q}: {e}")
            except Exception as e:
                errors.append(f"{q}: {type(e).__name__}: {e}")

        # Also fold high-volume from token profiles is optional; seed search is enough
        best = best_pair_per_token(all_pairs)
        candidates: list[PairQuote] = []
        for pq in best.values():
            sym = (pq.base_symbol or "").upper()
            if sym in MEME_EXCLUDE_SYMBOLS:
                continue
            # skip stock-like symbols in defaults
            if sym in {a.symbol.upper() for a in DEFAULT_RWA}:
                continue
            # skip obvious stables
            if sym.endswith("USD") and pq.price_usd and 0.95 < pq.price_usd < 1.05:
                continue
            candidates.append(pq)

        candidates.sort(key=lambda p: p.volume_h24, reverse=True)
        out: list[TrackedAsset] = []
        seen_syms: set[str] = set()
        for pq in candidates:
            sym = (pq.base_symbol or "").upper()
            if sym in seen_syms:
                continue
            seen_syms.add(sym)
            out.append(_from_pair(pq, "meme"))
            if len(out) >= limit:
                break

        if not out and errors:
            # surface discovery failure as a single error asset
            out.append(
                TrackedAsset(
                    symbol="—",
                    name="Memecoin discovery failed",
                    address="",
                    category="meme",
                    error="; ".join(errors)[:300],
                )
            )
        return out

    def fetch_custom(self) -> list[TrackedAsset]:
        if not self._custom_addresses:
            return []
        net = self.network
        try:
            pairs = tokens_by_address(self._custom_addresses, chain=net.dexscreener_slug)
            best = best_pair_per_token(pairs)
        except DexScreenerError as e:
            return [
                TrackedAsset(
                    symbol="?",
                    name="Custom",
                    address=a,
                    category="custom",
                    error=str(e),
                )
                for a in self._custom_addresses
            ]
        except Exception as e:
            return [
                TrackedAsset(
                    symbol="?",
                    name="Custom",
                    address=a,
                    category="custom",
                    error=f"{type(e).__name__}: {e}",
                )
                for a in self._custom_addresses
            ]

        out: list[TrackedAsset] = []
        for a in self._custom_addresses:
            pq = best.get(a.lower())
            if pq is None:
                out.append(
                    TrackedAsset(
                        symbol="?",
                        name="Unknown token",
                        address=a,
                        category="custom",
                        error="No pair on DexScreener for this chain",
                    )
                )
            else:
                out.append(_from_pair(pq, "custom"))
        return out

    def _discover_pairs(self, *, force: bool = False) -> list[PairQuote]:
        """Union of search results for volume/momentum rankings (cached ~90s)."""
        import time as _time

        net = self.network
        cache_key = net.dexscreener_slug
        now = _time.time()
        if not force and cache_key in _PAIR_CACHE:
            ts, pairs = _PAIR_CACHE[cache_key]
            if now - ts < _PAIR_CACHE_TTL_SEC and pairs:
                return list(pairs)

        all_pairs: list[PairQuote] = []
        # Parallel-ish: short sleep only between calls; fewer queries overall
        for q in _VOLUME_SEED_QUERIES:
            try:
                all_pairs.extend(search_pairs(q, chain=net.dexscreener_slug, timeout=12.0))
            except Exception:
                continue
            sleep_politely(0.05)
        pairs = list(best_pair_per_token(all_pairs).values())
        _PAIR_CACHE[cache_key] = (now, pairs)
        return pairs

    def fetch_top_volume(self, limit: int = 10) -> list[TrackedAsset]:
        """Top tokens by 24h volume (all categories)."""
        try:
            pairs = self._discover_pairs()
            pairs.sort(key=lambda p: p.volume_h24, reverse=True)
            out: list[TrackedAsset] = []
            seen: set[str] = set()
            for pq in pairs:
                key = (pq.base_address or "").lower()
                if not key or key in seen:
                    continue
                seen.add(key)
                out.append(_from_pair(pq, "top_volume"))
                if len(out) >= limit:
                    break
            if not out:
                return [
                    TrackedAsset(
                        symbol="—",
                        name="No volume data",
                        address="",
                        category="top_volume",
                        error="DexScreener returned no robinhood pairs",
                    )
                ]
            return out
        except Exception as e:
            return [
                TrackedAsset(
                    symbol="ERR",
                    name="Top volume failed",
                    address="",
                    category="top_volume",
                    error=f"{type(e).__name__}: {e}",
                )
            ]

    def fetch_trending_boosts(self, limit: int = 10) -> list[TrackedAsset]:
        """DexScreener token-boosts (spotlight) on this chain, with prices."""
        net = self.network
        try:
            boosts = token_boosts(which="top", chain=net.dexscreener_slug)
            # fill from latest if top is thin
            if len(boosts) < limit:
                seen = {str(b.get("tokenAddress") or "").lower() for b in boosts}
                for b in token_boosts(which="latest", chain=net.dexscreener_slug):
                    addr = str(b.get("tokenAddress") or "").lower()
                    if addr and addr not in seen:
                        boosts.append(b)
                        seen.add(addr)
                    if len(boosts) >= limit:
                        break
            addrs = [
                str(b.get("tokenAddress") or "")
                for b in boosts
                if is_valid_evm_address(str(b.get("tokenAddress") or ""))
            ][: max(limit, 15)]
            quotes = best_pair_per_token(
                tokens_by_address(addrs, chain=net.dexscreener_slug) if addrs else []
            )
            out: list[TrackedAsset] = []
            for b in boosts:
                if len(out) >= limit:
                    break
                addr = str(b.get("tokenAddress") or "")
                note = str(b.get("description") or "DexScreener boost")[:120]
                pq = quotes.get(addr.lower()) if addr else None
                if pq:
                    t = _from_pair(pq, "trending_boosts", note=note)
                    out.append(t)
                else:
                    out.append(
                        TrackedAsset(
                            symbol="?",
                            name="Boosted token",
                            address=addr,
                            category="trending_boosts",
                            note=note,
                            error="No liquid pair quote yet" if addr else "Missing address",
                        )
                    )
            if not out:
                out.append(
                    TrackedAsset(
                        symbol="—",
                        name="No boosts",
                        address="",
                        category="trending_boosts",
                        error="No Robinhood boosts on DexScreener right now",
                    )
                )
            return out
        except Exception as e:
            return [
                TrackedAsset(
                    symbol="ERR",
                    name="Boosts failed",
                    address="",
                    category="trending_boosts",
                    error=f"{type(e).__name__}: {e}",
                )
            ]

    def fetch_trending_momentum(
        self,
        limit: int = 10,
        *,
        min_vol: float = 5_000.0,
        min_liq: float = 2_000.0,
    ) -> list[TrackedAsset]:
        """Biggest |24h %| movers with volume/liquidity filters."""
        try:
            pairs = self._discover_pairs()
            active = [
                p
                for p in pairs
                if p.volume_h24 >= min_vol
                and p.liquidity_usd >= min_liq
                and p.price_change_h24 is not None
            ]
            active.sort(key=lambda p: abs(p.price_change_h24 or 0.0), reverse=True)
            out: list[TrackedAsset] = []
            seen: set[str] = set()
            for pq in active:
                key = (pq.base_address or "").lower()
                if not key or key in seen:
                    continue
                seen.add(key)
                out.append(_from_pair(pq, "trending_momentum"))
                if len(out) >= limit:
                    break
            if not out:
                return [
                    TrackedAsset(
                        symbol="—",
                        name="No momentum movers",
                        address="",
                        category="trending_momentum",
                        error="No pairs met vol/liq filters",
                    )
                ]
            return out
        except Exception as e:
            return [
                TrackedAsset(
                    symbol="ERR",
                    name="Momentum failed",
                    address="",
                    category="trending_momentum",
                    error=f"{type(e).__name__}: {e}",
                )
            ]

    def fetch_category(self, category_id: str, limit: int = 10) -> list[TrackedAsset]:
        """Dispatch by category id (dropdown). Never raises."""
        cat = (category_id or DEFAULT_CATEGORY).lower().strip()
        try:
            if cat == "top_volume":
                return self.fetch_top_volume(limit=limit)
            if cat == "trending_boosts":
                return self.fetch_trending_boosts(limit=limit)
            if cat == "trending_momentum":
                return self.fetch_trending_momentum(limit=limit)
            if cat == "memecoins":
                return self.fetch_top_memecoins(limit=limit)
            if cat == "rwa":
                return self.fetch_rwa()
            if cat == "custom":
                return self.fetch_custom()
            return self.fetch_top_volume(limit=limit)
        except Exception as e:
            return [
                TrackedAsset(
                    symbol="ERR",
                    name=cat,
                    address="",
                    category=cat,
                    error=f"{type(e).__name__}: {e}",
                )
            ]

    def fetch_all(self, meme_limit: int = 10) -> dict[str, list[TrackedAsset]]:
        """Fetch all sections; isolates failures."""
        rwa: list[TrackedAsset] = []
        memes: list[TrackedAsset] = []
        custom: list[TrackedAsset] = []
        try:
            rwa = self.fetch_rwa()
        except Exception as e:
            rwa = [
                TrackedAsset(
                    symbol="ERR",
                    name="RWA section crash",
                    address="",
                    category="rwa",
                    error=f"{type(e).__name__}: {e}",
                )
            ]
        try:
            memes = self.fetch_top_memecoins(limit=meme_limit)
        except Exception as e:
            memes = [
                TrackedAsset(
                    symbol="ERR",
                    name="Meme section crash",
                    address="",
                    category="meme",
                    error=f"{type(e).__name__}: {e}",
                )
            ]
        try:
            custom = self.fetch_custom()
        except Exception as e:
            custom = [
                TrackedAsset(
                    symbol="ERR",
                    name="Custom section crash",
                    address="",
                    category="custom",
                    error=f"{type(e).__name__}: {e}",
                )
            ]
        return {"rwa": rwa, "meme": memes, "custom": custom}
