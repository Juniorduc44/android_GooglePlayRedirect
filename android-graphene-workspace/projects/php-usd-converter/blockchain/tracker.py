"""Orchestrate RWA defaults + top memecoins + custom contracts."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from .defaults import DEFAULT_RWA, MEME_EXCLUDE_SYMBOLS, MEME_SEED_QUERIES
from .dexscreener import (
    DexScreenerError,
    PairQuote,
    best_pair_per_token,
    is_valid_evm_address,
    search_pairs,
    sleep_politely,
    tokens_by_address,
)
from .networks import DEFAULT_NETWORK_ID, Network, get_network


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
            "pair_address": self.pair_address,
            "dex_id": self.dex_id,
            "url": self.url,
            "error": self.error,
            "note": self.note,
        }


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
