# Plan: Spec tab — item / token price perspectives

**Status:** implemented (v1.8.1 — Dex import + field locks)  
**Date:** 2026-07-30  
**Goal:** One section to reason about **buying, selling, and speculating** on an “item” (token, collectible, stock unit). Optional **DexScreener import** with per-field locks for what-if scenarios.

---

## 1. What you asked for (mapped to tools)

| Your idea | Math | Tool name in app |
|---|---|---|
| Price of an item from **market cap** + **how many items exist** | `price = marketCap / supply` | **Price from mcap** |
| How many items someone got for **money spent** at a **price** | `items = spent / price` | **Buy · spent → items** |
| Total value of **holdings** at a **desired price** | `value = holdings × targetPrice` | **Portfolio · target value** |
| Pull a **trending Dex token**, **keep supply**, **override mcap** | Live snapshot + locks | **0 · From DexScreener** |

Extra reverse formulas (same panels, free):

- `marketCap = price × supply` (shown under Price panel)
- `spent = items × price` (shown under Buy panel)
- Optional: if you spent $X for N items, **avg cost** = spent / items (shown under Portfolio when spend filled)
- **Estimated supply** when Dex omits raw supply: `supply ≈ marketCap ÷ price` (else `fdv ÷ price`)

All values are **unit-agnostic** (treat as USD-like; label as “$” for readability). Wallet not required.

---

## 2. UX

- Menu label: **Spec**
- Subtitle: *Buy · sell · speculate*
- One scroll screen:
  1. **From DexScreener** — load list, pick token, lock/unlock supply · mcap · price  
  2. **Price from mcap**  
  3. **Buy**  
  4. **Portfolio**
- **Default locks:** supply ✅ locked · market cap ❌ free (what-if) · price ❌ free  
- Checked = field **disabled** and refilled from the live token snapshot  
- Unchecked = you type freely (e.g. custom market cap on a 1B supply token)  
- Live recalculation on keystroke (desktop + Android)
- Compact number format + `1.5b` / `50m` / `250k` shortcuts

**Not in scope:** wallet, historical charts, fees/slippage.

---

## 3. Formulas (shared)

```
price_from_mcap(mcap, supply)     = mcap / supply
mcap_from_price(price, supply)    = price * supply
items_from_spend(spent, price)    = spent / price
cost_for_items(price, items)      = price * items
value_at_target(holdings, target) = holdings * target
avg_cost(spent, holdings)         = spent / holdings   # if both set
pnl(holdings, target, spent)      = value_at_target - spent  # if spent set
```

Guard: division by zero → clear status message, no crash.

---

## 4. Implementation

| Layer | Location |
|---|---|
| Desktop math | `speculator/math_fmt.py` |
| Desktop UI | `app.py` → `_build_spec_tab` |
| Android math | `SpeculatorMath.kt` |
| Android UI | `activity_main.xml` + `MainActivity` |
| Nav index | after **Temp**, before **Chain** |

---

## 5. Acceptance

- [x] Three calculators on one Spec section  
- [x] Desktop + Android  
- [x] DexScreener load (trending / volume / meme / RWA)  
- [x] Per-field locks: supply / mcap / price  
- [x] Estimated supply from mcap÷price when needed  
- [x] Safe empty / zero / invalid input handling  

### Example what-if flow

1. Load **Trending · boosts**  
2. Select a token with ~**1,000,000,000** supply  
3. Leave **Supply** checked (locked)  
4. Uncheck **Market cap** → type `100m` (or any scenario)  
5. Panel 1 shows new **price = your mcap ÷ locked supply**  
6. Copy price → Buy / Portfolio panels as needed  

---
