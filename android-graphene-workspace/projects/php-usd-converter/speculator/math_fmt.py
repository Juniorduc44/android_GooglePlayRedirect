"""Pure math + formatting for Spec tab (no network)."""

from __future__ import annotations

import re
from typing import Optional


def parse_number(raw: str | None) -> Optional[float]:
    """Parse user input; allows commas, spaces, $ and k/m/b suffixes."""
    if raw is None:
        return None
    s = str(raw).strip().lower().replace(",", "").replace("$", "").replace(" ", "")
    if not s or s in (".", "-", "+"):
        return None
    mult = 1.0
    if s.endswith("b"):
        mult = 1e9
        s = s[:-1]
    elif s.endswith("m"):
        mult = 1e6
        s = s[:-1]
    elif s.endswith("k"):
        mult = 1e3
        s = s[:-1]
    # strip trailing junk
    s = re.sub(r"[^0-9.eE+-]", "", s)
    if not s or s in (".", "-", "+", "e", "E"):
        return None
    try:
        return float(s) * mult
    except ValueError:
        return None


def price_from_mcap(market_cap: float, supply: float) -> float:
    if supply == 0:
        raise ZeroDivisionError("supply")
    return market_cap / supply


def mcap_from_price(price: float, supply: float) -> float:
    return price * supply


def items_from_spend(spent: float, price: float) -> float:
    if price == 0:
        raise ZeroDivisionError("price")
    return spent / price


def cost_for_items(price: float, items: float) -> float:
    return price * items


def value_at_target(holdings: float, target_price: float) -> float:
    return holdings * target_price


def avg_cost(spent: float, holdings: float) -> float:
    if holdings == 0:
        raise ZeroDivisionError("holdings")
    return spent / holdings


def pnl_at_target(holdings: float, target_price: float, spent: float) -> float:
    return value_at_target(holdings, target_price) - spent


def format_money(value: float) -> str:
    """USD-style money for mcap / portfolio (compact when large)."""
    av = abs(value)
    sign = "-" if value < 0 else ""
    if av >= 1e12:
        return f"{sign}${av / 1e12:.3f}T"
    if av >= 1e9:
        return f"{sign}${av / 1e9:.3f}B"
    if av >= 1e6:
        return f"{sign}${av / 1e6:.3f}M"
    if av >= 1e3:
        return f"{sign}${av:,.2f}"
    if av >= 1:
        return f"{sign}${av:,.4f}"
    if av >= 1e-4:
        return f"{sign}${av:.6f}"
    if av == 0:
        return "$0"
    return f"{sign}${av:.4e}"


def estimate_supply(
    price: float | None,
    market_cap: float | None = None,
    fdv: float | None = None,
) -> float | None:
    """Infer circulating-ish supply from DexScreener mcap or FDV ÷ price.

    DexScreener does not always publish raw supply; mcap/price (or fdv/price)
    is the practical estimate for what-if calculators.
    """
    if price is None or price <= 0:
        return None
    for cap in (market_cap, fdv):
        if cap is not None and cap > 0:
            return cap / price
    return None


def format_qty(value: float) -> str:
    """Quantity of items / tokens."""
    av = abs(value)
    sign = "-" if value < 0 else ""
    if av >= 1e12:
        return f"{sign}{av / 1e12:.3f}T"
    if av >= 1e9:
        return f"{sign}{av / 1e9:.3f}B"
    if av >= 1e6:
        return f"{sign}{av / 1e6:.3f}M"
    if av >= 1e3:
        return f"{sign}{av:,.2f}"
    if av >= 1:
        return f"{sign}{av:,.4f}"
    if av >= 1e-6:
        return f"{sign}{av:.8f}".rstrip("0").rstrip(".")
    if av == 0:
        return "0"
    return f"{sign}{av:.4e}"
