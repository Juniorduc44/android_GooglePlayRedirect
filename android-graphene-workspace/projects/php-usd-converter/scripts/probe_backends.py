#!/usr/bin/env python3
"""CLI: probe every translation backend and print results before UI testing.

Usage (from project root):
  ./venv/bin/python scripts/probe_backends.py
  ./venv/bin/python scripts/probe_backends.py --text "Hello" --lang Spanish
  ./venv/bin/python scripts/probe_backends.py --only google,mymemory,hf_opus
  ./venv/bin/python scripts/probe_backends.py --skip-slow   # skip local HF first-download
  ./venv/bin/python scripts/probe_backends.py --phonics
"""

from __future__ import annotations

import argparse
import sys
import time
import traceback
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from translator.backends import BACKEND_IDS, BACKEND_LABELS, get_backend  # noqa: E402
from translator.secrets_store import SecretsStore  # noqa: E402

# Local HF downloads can be slow first time
SLOW_IDS = {"hf_opus", "hf_t5"}

# Sample targets that exercise each engine well
DEFAULT_CASES = [
    ("Spanish", "Hello, how much does this cost?"),
    ("French", "Where is the train station?"),
    ("German", "Good morning"),
]


def main() -> int:
    ap = argparse.ArgumentParser(description="Probe translator backends")
    ap.add_argument("--text", default="", help="Override source text")
    ap.add_argument("--lang", default="", help="Override target language name")
    ap.add_argument(
        "--only",
        default="",
        help="Comma-separated backend ids (default: all)",
    )
    ap.add_argument(
        "--skip-slow",
        action="store_true",
        help="Skip local HF models (first download can take minutes)",
    )
    ap.add_argument(
        "--phonics",
        action="store_true",
        help="Also run gooble-style phonics on English sample",
    )
    ap.add_argument(
        "--timeout-note",
        action="store_true",
        help="No-op flag for docs",
    )
    args = ap.parse_args()

    store = SecretsStore()
    only = {x.strip() for x in args.only.split(",") if x.strip()} or set(BACKEND_IDS)

    print("=" * 72)
    print("Translator backend probe")
    print(f"Project: {ROOT}")
    print(f"Active secrets backend: {store.get('active_backend')}")
    print("=" * 72)

    results: list[tuple[str, str, str, float, str]] = []
    # id, stage, status, seconds, detail

    for bid in BACKEND_IDS:
        if bid not in only:
            continue
        if args.skip_slow and bid in SLOW_IDS:
            results.append((bid, "skip", "SKIP", 0.0, "slow local HF (--skip-slow)"))
            print(f"\n## {bid} — {BACKEND_LABELS.get(bid, bid)}  [SKIP slow]")
            continue

        label = BACKEND_LABELS.get(bid, bid)
        print(f"\n## {bid} — {label}")
        try:
            backend = get_backend(store, bid)
        except Exception as e:
            results.append((bid, "init", "FAIL", 0.0, str(e)[:120]))
            print(f"  INIT FAIL: {e}")
            continue

        t0 = time.time()
        try:
            ok, msg = backend.available()
        except Exception as e:
            ok, msg = False, str(e)
        dt = time.time() - t0
        print(f"  available: {ok}  ({dt:.2f}s)  {msg}")
        results.append((bid, "available", "OK" if ok else "NO", dt, msg[:160]))
        if not ok:
            continue

        # translate cases
        if args.text and args.lang:
            cases = [(args.lang, args.text)]
        else:
            cases = list(DEFAULT_CASES)
            # Opus/T5 have limited pairs — prefer matching langs
            if bid == "hf_t5":
                cases = [
                    ("German", "Hello, how much does this cost?"),
                    ("French", "Where is the train station?"),
                ]
            elif bid == "hf_opus":
                cases = [
                    ("Spanish", "Hello, how much does this cost?"),
                    ("French", "Where is the train station?"),
                    ("German", "Good morning"),
                ]

        for lang, text in cases:
            t0 = time.time()
            try:
                out = backend.translate(text, lang)
                dt = time.time() - t0
                status = "PASS" if out and out.strip() else "EMPTY"
                detail = (out or "")[:200].replace("\n", " ")
                print(f"  translate → {lang}: [{status} {dt:.2f}s] {detail}")
                results.append((bid, f"tr:{lang}", status, dt, detail))
            except Exception as e:
                dt = time.time() - t0
                err = f"{type(e).__name__}: {e}"
                print(f"  translate → {lang}: [FAIL {dt:.2f}s] {err[:200]}")
                results.append((bid, f"tr:{lang}", "FAIL", dt, err[:160]))
                if "--debug" in sys.argv:
                    traceback.print_exc()

        if args.phonics:
            sample = "Hello, how much does this cost?"
            t0 = time.time()
            try:
                out = backend.phonetics(sample, "English")
                dt = time.time() - t0
                detail = (out or "")[:200].replace("\n", " ")
                print(f"  phonics: [PASS {dt:.2f}s] {detail}")
                results.append((bid, "phonics", "PASS", dt, detail))
            except Exception as e:
                dt = time.time() - t0
                err = f"{type(e).__name__}: {e}"
                print(f"  phonics: [FAIL {dt:.2f}s] {err[:200]}")
                results.append((bid, "phonics", "FAIL", dt, err[:160]))

    # summary table
    print("\n" + "=" * 72)
    print(f"{'backend':<14} {'stage':<14} {'status':<6} {'sec':>6}  detail")
    print("-" * 72)
    for bid, stage, status, dt, detail in results:
        print(f"{bid:<14} {stage:<14} {status:<6} {dt:6.2f}  {detail[:40]}")
    print("=" * 72)

    # exit code: fail if none of the free/local engines passed a translate
    translate_pass = [
        r
        for r in results
        if r[1].startswith("tr:") and r[2] == "PASS"
    ]
    freeish = {"google", "mymemory", "hf_opus", "hf_t5"}
    free_pass = [r for r in translate_pass if r[0] in freeish]
    print(f"\nTranslate PASS count: {len(translate_pass)}  (free/local: {len(free_pass)})")
    if not free_pass and not translate_pass:
        print("RESULT: no backend produced a translation — check network / installs")
        return 1
    print("RESULT: at least one backend works — safe to try the UI")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
