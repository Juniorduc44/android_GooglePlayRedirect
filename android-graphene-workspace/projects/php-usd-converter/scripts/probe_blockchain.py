#!/usr/bin/env python3
"""CLI: probe Robinhood Chain price tracker before using the UI.

  ./venv/bin/python scripts/probe_blockchain.py
  ./venv/bin/python scripts/probe_blockchain.py --offline-unit
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from blockchain.selftest import run_selftests, summarize  # noqa: E402
from blockchain.tracker import PriceTracker  # noqa: E402


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument(
        "--offline-unit",
        action="store_true",
        help="Skip live DexScreener calls",
    )
    ap.add_argument("--show-tables", action="store_true", help="Print RWA + meme tables")
    args = ap.parse_args()

    print("=" * 72)
    print("Blockchain / Robinhood Chain probe")
    print("=" * 72)

    results = run_selftests(live_network=not args.offline_unit)
    passed, failed, text = summarize(results)
    print(text)

    if args.show_tables and not args.offline_unit:
        tr = PriceTracker("robinhood")
        print("\n--- RWA ---")
        for a in tr.fetch_rwa():
            print(
                f"{a.symbol:8} {a.format_price():>14}  {a.format_change():>8}  "
                f"{a.short_contract()}  {a.error or ''}"
            )
        print("\n--- Top memes ---")
        for a in tr.fetch_top_memecoins(10):
            print(
                f"{a.symbol:12} {a.format_price():>14}  vol24={a.volume_h24:,.0f}  "
                f"{a.short_contract()}  {a.error or ''}"
            )

    print("=" * 72)
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
