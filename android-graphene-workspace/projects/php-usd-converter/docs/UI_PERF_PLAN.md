# Plan: Chain viewer polish + faster app load

**Status:** approved direction for next work  
**Date:** 2026-07-29  
**Product shift:** Wallet is **removed**. App is a **multi-tool toolkit** with **Robinhood Chain markets viewer** (read-only prices/contracts). No custody, keys, or passkey work.

---

## 1. Goals

| Priority | Goal |
|---|---|
| P0 | **Fast cold start** — UI interactive in well under 1s on mid-range phones / desktop |
| P0 | **Fast Chain open** — market list useful in &lt;2s on warm cache, &lt;5s cold when network OK |
| P1 | **Cleaner Chain UI** — denser, clearer hierarchy; feels like a market viewer, not a form dump |
| P1 | **Snappy navigation** — sandwich menu + section switch with zero lag |
| P2 | **Less jank** — no main-thread network, no rebuild-everything on every refresh |

**Non-goals (explicit):**

- No wallet, keys, send, sign, or passkey
- No hood.dev clone / auth
- No heavy redesign of Convert/Travel/Weight/Temp beyond shared chrome consistency

---

## 2. Done already (this change set)

- **Wallet stripped** from desktop (`app.py`, `wallet/` package, `eth-account` dep)
- **Wallet stripped** from Android (layout tab, menu entry, `LocalWalletStore`, `ChainRpc`, web3j)
- Chain remains the **Robinhood markets viewer** only
- Nav sections: Convert · Travel · Weight · Temp · Blockchain · Translator (desktop) · Settings

---

## 3. Current speed bottlenecks (measured architecture)

### 3.1 Desktop cold start

| Step | Problem |
|---|---|
| `self.exchange_rate = self.get_live_rate()` in `__init__` | **Blocks** window creation on HTTP (up to 5s timeout) |
| `_build_ui()` builds **all** sections at once | Converts, Travel, Weight, Temp, Blockchain cards, Translator, Settings — all before first paint |
| Translator / secrets init | Light, but still on main path |
| Heavy optional deps (`torch`, `transformers`) | Only if translator HF path used — keep **lazy** forever |

### 3.2 Android cold start

| Step | Problem |
|---|---|
| Inflating large `activity_main.xml` | One giant `ViewFlipper` with every section’s full tree |
| `setupChainTab()` + empty card render | Fine; **good** |
| FX rate coroutine | Already async — keep pattern |
| web3j (removed) | Was APK size + init cost — **gone** |

### 3.3 Chain data load

| Step | Problem |
|---|---|
| DexScreener multi-seed discovery | Sequential HTTP for volume/meme views (even lean seeds) |
| 25s default timeouts | One hung call feels frozen |
| Cache TTL 90s in-process only | Lost on process death; reopen feels cold again |
| Auto-refresh on first Chain open | Correct, but must show **skeleton + stale** immediately |

---

## 4. Performance plan (ordered by impact)

### Phase F1 — Instant shell (P0) · estimate: small

**Desktop**

1. Open window first; show Convert with “rate loading…”
2. Fetch FX rate on a **background thread**; never block `__init__`
3. **Lazy-build** section frames: only build Convert + Settings chrome at start; build Travel/Weight/Temp/Blockchain/Translator **on first open**

**Android**

1. Keep single Activity, but optionally split Chain into `include` + delayed inflate, or leave as-is if F1 desktop is enough
2. Confirm no main-thread network (already coroutines)
3. Measure cold start with Android Studio / simple log timestamps

**Success:** First interactive frame &lt;300ms desktop typical; FX fills in without stalling UI.

### Phase F2 — Chain cache + parallel fetch (P0) · estimate: medium

1. **Disk cache** (JSON) for last successful market payload per category (desktop `~/.cache` or app dir; Android `filesDir` / SharedPreferences for meta + file for body)
2. On Chain open: **paint cache immediately** → refresh in background → swap when ready
3. Cut DexScreener timeout to **8–12s**; fail soft with retry
4. Parallelize seed searches with a small thread/coroutine pool (e.g. 4 workers) + polite rate limit
5. Prefer **tokens/v1 batch** for known addresses (RWA defaults) — already good path; make default category RWA/top volume that uses batch first
6. Raise in-memory TTL only if disk cache exists; keep 60–120s memory layer

**Success:** Second open of Chain is near-instant; cold open shows data immediately if visited before.

### Phase F3 — Leaner market queries (P0/P1)

1. Default view: **RWA batch** or **Top volume from one search** — not full multi-seed meme discovery
2. Heavy views (Memecoins, Trending) only when user picks them
3. Cap list at 10–15 rows; “Load more” optional later
4. Self-test: **manual only**, never on open; keep CLI probe for CI

### Phase F4 — UI polish as Chain viewer (P1)

**Visual hierarchy**

```
┌ Header: Chain · Robinhood · 4663 · LIVE ─┐
│ [View ▾ Top volume]  [Refresh]            │
│ Optional: search / add contract           │
├───────────────────────────────────────────┤
│ Rank  Symbol   Price    24h     Vol       │  denser rows or compact cards
│  1    NVDA    $xxx    +x.x%   $xm         │
│  …                                        │
└───────────────────────────────────────────┘
```

**Concrete UI work**

| Item | Detail |
|---|---|
| Hero slim | One line meta: chain id + LIVE/STALE age (“updated 12s ago”) |
| Compact rows | Prefer list row over large card for 10+ assets; keep card for detail expand optional |
| Color language | Green/red 24h change; muted contracts; mono only for addresses |
| Empty / loading | Skeleton 6 shimmer rows (or simple gray bars), not blank |
| Error | Inline banner + Retry; never stack traces in main view |
| Custom contract | Collapsed “Track contract” expander |
| Self-test | Settings or overflow “Diagnostics” — out of primary path |
| Menu labels | “Chain” not “Blockchain”; subtitle “Markets · Robinhood” |

**Shared chrome (light)**

- Consistent header height, card radius, accent (`#3B82F6` / slate dark already)
- Touch targets ≥48dp on Android; desktop buttons 36–40px height

### Phase F5 — Startup / APK fat (P1)

| Item | Action |
|---|---|
| Desktop `requirements.txt` | Keep `torch`/`transformers` **optional** extras (`requirements-ml.txt`) so default install is lean |
| Android APK | Already dropped web3j; enable R8 minify if not on; strip unused resources |
| Translator | Fully lazy import backends; never import HF at app start |
| Passkey research clones | Stay under `tools/` only — never ship in APK |

### Phase F6 — Optional later

- Pull-to-refresh on Chain
- Sort by volume / change
- Persist last scroll position
- DexScreener CDN / mirror if public API throttles
- Desktop: don’t start Translator secrets UI until opened

---

## 5. Implementation sequence (recommended)

| Step | Deliverable | Ships alone? |
|---|---|---|
| 0 | Wallet removed (this PR / change) | Yes |
| 1 | F1 instant shell (async FX + lazy sections) | Yes — biggest perceived speed win |
| 2 | F2 disk cache + paint-stale-first | Yes |
| 3 | F3 default lean category + parallel seeds | Yes |
| 4 | F4 Chain UI denser viewer | Yes |
| 5 | F5 deps/APK slim | Yes |

Do **not** combine all into one giant release unless you want one big test pass. Prefer **1.8.x** strip, then **1.8.1** speed, **1.8.2** UI.

---

## 6. Acceptance checks

**Speed**

- [ ] Desktop: window appears before any network returns
- [ ] Android: time-to-interactive log &lt;1s cold on mid device (target)
- [ ] Chain with disk cache: content visible in &lt;200ms after section open
- [ ] Chain cold network: status “Updating…” + skeleton, no freezes

**Product**

- [ ] No menu entry, layout, or strings for Wallet
- [ ] No private key / password wallet code paths
- [ ] Chain is clearly “markets viewer” copy only

**Regressions**

- [ ] Convert / Travel / Weight / Temp still work
- [ ] Chain Refresh / category switch / custom contract / self-test (if kept) work
- [ ] Settings result text size still applies

---

## 7. Copy / product framing

Use:

- “**Chain · Robinhood markets**”
- “Prices via DexScreener · not trading · not a wallet”

Avoid:

- “Connect wallet”, “Create wallet”, “Passkey”, “Sign”, “Custody”

---

## 8. Out of scope forever (unless product changes again)

- Embedded wallets, Turnkey, passkeys, seed import
- Sending ETH/tokens
- Brokerage / Robinhood account login

Passkey research under `tools/passkey-research/` remains historical only.

---

## 9. Immediate next action after this plan

Implement **Phase F1** (async FX + lazy section build) on desktop, then **F2 cache** on desktop + Android Chain. That is the highest ROI for “faster the better.”
