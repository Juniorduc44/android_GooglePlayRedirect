"""Extensive self-tests for blockchain price tracker (try/except heavy)."""

from __future__ import annotations

import time
import traceback
from dataclasses import dataclass
from typing import Callable

from .defaults import DEFAULT_RWA
from .dexscreener import (
    DexScreenerError,
    is_valid_evm_address,
    search_pairs,
    tokens_by_address,
)
from .networks import DEFAULT_NETWORK_ID, NETWORKS, get_network
from .tracker import PriceTracker


@dataclass
class TestResult:
    name: str
    ok: bool
    detail: str
    seconds: float

    def line(self) -> str:
        flag = "PASS" if self.ok else "FAIL"
        return f"[{flag}] {self.name} ({self.seconds:.2f}s) — {self.detail}"


def _run(name: str, fn: Callable[[], str]) -> TestResult:
    t0 = time.time()
    try:
        detail = fn()
        return TestResult(name=name, ok=True, detail=detail, seconds=time.time() - t0)
    except Exception as e:
        tb = traceback.format_exc(limit=2)
        return TestResult(
            name=name,
            ok=False,
            detail=f"{type(e).__name__}: {e} | {tb.splitlines()[-1] if tb else ''}",
            seconds=time.time() - t0,
        )


def run_selftests(*, live_network: bool = True) -> list[TestResult]:
    """Run unit + optional live DexScreener checks. Never raises."""
    results: list[TestResult] = []

    def t_networks() -> str:
        assert DEFAULT_NETWORK_ID == "robinhood"
        n = get_network()
        assert n.chain_id == 4663
        assert n.dexscreener_slug == "robinhood"
        assert "robinhood" in NETWORKS
        # unknown raises
        try:
            get_network("not-a-chain")
            raise AssertionError("expected KeyError")
        except KeyError:
            pass
        return f"default {n.name} chain_id={n.chain_id}"

    results.append(_run("network registry", t_networks))

    def t_address_validation() -> str:
        assert is_valid_evm_address("0xd0601CE157Db5bdC3162BbaC2a2C8aF5320D9EEC")
        assert not is_valid_evm_address("0x123")
        assert not is_valid_evm_address("")
        assert not is_valid_evm_address("not-hex")
        return "EVM address regex OK"

    results.append(_run("contract address validation", t_address_validation))

    def t_defaults() -> str:
        assert len(DEFAULT_RWA) >= 5
        for a in DEFAULT_RWA:
            assert is_valid_evm_address(a.address), a.symbol
            assert a.category == "rwa"
        syms = {a.symbol for a in DEFAULT_RWA}
        assert {"NVDA", "TSLA", "AAPL", "GOOGL", "MSFT"} <= syms
        return f"{len(DEFAULT_RWA)} RWA defaults: {', '.join(sorted(syms))}"

    results.append(_run("default RWA catalog", t_defaults))

    def t_tracker_custom() -> str:
        tr = PriceTracker()
        try:
            tr.add_custom_contract("bad")
            raise AssertionError("should reject")
        except ValueError:
            pass
        a = tr.add_custom_contract("0xd0601CE157Db5bdC3162BbaC2a2C8aF5320D9EEC")
        assert a in tr.list_custom()
        tr.clear_custom()
        assert tr.list_custom() == []
        return "custom contract add/clear OK"

    results.append(_run("custom contract bookkeeping", t_tracker_custom))

    if not live_network:
        results.append(
            TestResult("live network skipped", True, "live_network=False", 0.0)
        )
        return results

    def t_search() -> str:
        pairs = search_pairs("NVDA", chain="robinhood")
        if not pairs:
            raise DexScreenerError("search NVDA returned 0 robinhood pairs")
        hit = pairs[0]
        assert hit.base_address
        assert hit.price_usd is not None and hit.price_usd > 0
        return (
            f"{hit.base_symbol} ${hit.price_usd} "
            f"contract={hit.base_address[:10]}… pair={hit.pair_address[:10]}…"
        )

    results.append(_run("DexScreener search NVDA", t_search))

    def t_rwa_batch() -> str:
        addrs = [a.address for a in DEFAULT_RWA]
        pairs = tokens_by_address(addrs, chain="robinhood")
        if len(pairs) < 3:
            raise DexScreenerError(f"expected ≥3 pairs, got {len(pairs)}")
        priced = [p for p in pairs if p.price_usd]
        if len(priced) < 3:
            raise DexScreenerError("too few priced pairs")
        return f"{len(pairs)} pairs, {len(priced)} with priceUsd"

    results.append(_run("DexScreener tokens/v1 RWA batch", t_rwa_batch))

    def t_tracker_rwa() -> str:
        tr = PriceTracker("robinhood")
        rows = tr.fetch_rwa()
        assert len(rows) >= 5
        ok = [r for r in rows if r.error is None and r.price_usd]
        if len(ok) < 3:
            errs = "; ".join(f"{r.symbol}:{r.error}" for r in rows if r.error)
            raise DexScreenerError(f"only {len(ok)} RWA priced; {errs}")
        sample = ", ".join(f"{r.symbol}={r.format_price()}" for r in ok[:5])
        return sample

    results.append(_run("PriceTracker.fetch_rwa", t_tracker_rwa))

    def t_tracker_memes() -> str:
        tr = PriceTracker("robinhood")
        rows = tr.fetch_top_memecoins(limit=10)
        priced = [r for r in rows if r.error is None and r.address]
        if len(priced) < 5:
            raise DexScreenerError(
                f"expected ≥5 memecoins, got {len(priced)}; "
                + "; ".join(r.error or "" for r in rows if r.error)[:200]
            )
        # contracts present
        for r in priced:
            assert is_valid_evm_address(r.address), r.symbol
        return f"{len(priced)} memes e.g. {priced[0].symbol} {priced[0].format_price()}"

    results.append(_run("PriceTracker.fetch_top_memecoins", t_tracker_memes))

    def t_fetch_all() -> str:
        tr = PriceTracker()
        data = tr.fetch_all(meme_limit=10)
        assert "rwa" in data and "meme" in data
        return (
            f"rwa={len(data['rwa'])} meme={len(data['meme'])} "
            f"custom={len(data['custom'])}"
        )

    results.append(_run("PriceTracker.fetch_all", t_fetch_all))

    return results


def summarize(results: list[TestResult]) -> tuple[int, int, str]:
    passed = sum(1 for r in results if r.ok)
    failed = sum(1 for r in results if not r.ok)
    lines = [r.line() for r in results]
    lines.append(f"TOTAL {passed} passed, {failed} failed of {len(results)}")
    return passed, failed, "\n".join(lines)
