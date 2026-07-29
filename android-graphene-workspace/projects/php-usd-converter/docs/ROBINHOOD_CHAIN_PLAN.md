# Plan: Robinhood Chain access, price tracker & passkey wallet

**Status:** research + Phase 1 shipped (price tracker tab). Passkey wallet is **not** implemented yet.  
**Date:** 2026-07-29  
**Sources:** `docs/Robinhood.pdf` (Crypto Trading API), [docs.robinhood.com/chain](https://docs.robinhood.com/chain/), [hood.dev](https://hood.dev/), [DexScreener API](https://docs.dexscreener.com/api/reference), local `tools/browser-harness`.

---

## 1. What we learned

### 1.1 Robinhood.pdf (workspace docs)

The PDF is the **Robinhood Crypto Trading API** (US-only account trading: orders, balances, Ed25519 `x-api-key` / `x-signature` / `x-timestamp`). It is **not** the Robinhood Chain L2 developer guide.

| PDF covers | PDF does **not** cover |
|---|---|
| Crypto trading REST API auth | Chain ID / RPC / Stock Token contracts |
| Orders, holdings, market data for RH crypto | Passkeys / WebAuthn wallet bootstrap |
| US customer agreement constraints | hood.dev launchpad |

**Implication:** Keep Crypto Trading API as a **future optional** path (user RH credentials). Chain price tracking must use **public L2 + DexScreener**, not the PDF API.

### 1.2 Robinhood Chain (public docs)

| Property | Value |
|---|---|
| Type | Permissionless EVM L2 (Arbitrum Orbit / dedicated chain) |
| Chain ID | **4663** (mainnet), 46630 (testnet) |
| Gas | **ETH** |
| Explorer | https://robinhoodchain.blockscout.com |
| Public RPC | `https://rpc.mainnet.chain.robinhood.com` (rate-limited; **403** from this host without provider key) |
| Recommended RPC | Alchemy `https://robinhood-mainnet.g.alchemy.com/v2/{KEY}` |
| AA | First-class **ERC-4337** (Alchemy gasless / programmable wallets) |
| Oracles | Chainlink |
| DEX | Uniswap (and others) |
| Stablecoin | USDG (Paxos) |

Stock Tokens are tokenized RWA equities on-chain (economic exposure; legal caveats per Robinhood disclosures).

### 1.3 hood.dev /launch (passkey / wallet research)

Observed public product:

- **hood.dev** is a **token launchpad** on Robinhood Chain (“Launch, trade, and scale tokens”).
- **/launch** creates an ERC-20 + Uniswap/Sushi V3 pool with LP locked, anti-sniper caps, deploy cost ~0.001 ETH.
- UI requires **Connect a wallet** to generate/deploy contract addresses (vanity ending optional).
- This is **not** the same surface as “one-click passkey → smart wallet for anyone globally,” but it is the fastest public path to **use** the chain for token ops.

**Passkey / AA direction (to clone later):**

1. **Robinhood Chain ERC-4337** + Alchemy Gasless Transaction Infrastructure (official).
2. **WebAuthn passkey** as owner key of a smart account (e.g. P256 / WebAuthn validators used by many AA wallets) — research specific contracts on RH chain via browser-harness on hood.dev login flows and Alchemy AA docs.
3. **browser-harness** (local tool, not in-app) to reverse-engineer hood.dev wallet connection UX, network add (4663), and any embedded passkey prompts without guessing from static HTML alone.

**Legal / product caution:** Do not claim “official Robinhood login” or clone trademarks. Build **compatible** EVM + AA wallet UX for RH chain; passkey is **user self-custody**, not RH brokerage auth.

### 1.4 DexScreener (live, verified 2026-07-29)

- Chain slug: **`robinhood`**
- Endpoints used:
  - `GET https://api.dexscreener.com/latest/dex/search?q={query}`
  - `GET https://api.dexscreener.com/tokens/v1/robinhood/{addr1,addr2,...}`
- Verified **RWA / stock-token style** pairs with real USD prices & contracts, e.g.:
  - NVDA, TSLA, AAPL, GOOGL, MSFT (addresses stored in app defaults)
- Verified **memecoins** ranked by 24h volume on chain (excluding stable/stock symbols for the meme list).
- Rate limits: ~60 rpm class endpoints — batch token addresses; cache UI refreshes.

### 1.5 browser-harness (local tools)

Cloned to:

```text
tools/browser-harness/     # git clone of browser-use/browser-harness
```

Purpose: **agent/local research** (CDP control of Chrome), **not** shipped inside the APK.

Install path (dev machine with Chrome):

```bash
# from tools/browser-harness README / install.md
uv tool install --python 3.12 --upgrade --force browser-harness
# enable chrome://inspect/#remote-debugging
browser-harness <<'PY'
print(page_info())
PY
```

Use later for: hood.dev passkey/wallet flows, Robinhood docs JS pages, visual verification of DexScreener.

---

## 2. Accessing Robinhood Chain for **data** (price tracker)

### Phase A — shipped now (no wallet)

| Layer | Approach |
|---|---|
| Market prices | DexScreener public API (`chainId=robinhood`) |
| Defaults | Curated **5 RWA/stock tokens** + live **top 10 memecoins** by 24h volume |
| Custom track | User pastes ERC-20 contract; resolve via `/tokens/v1/robinhood/{address}` |
| Chain metadata | Hard-coded network card (ID 4663, explorer, RPC URL display) |
| RPC eth_call | Optional later when Alchemy key available (public RPC often 403) |

### Phase B — richer data

- Alchemy Data API (balances, transfers) with user key in `secrets.json`
- Blockscout REST for holders / token metadata
- CoinGecko if they list RH chain IDs
- Optional: RH Crypto Trading API from PDF for **brokerage** crypto (US only) — separate from chain

### Phase C — passkey smart wallet (plan only)

1. Map hood.dev connect flow with browser-harness (screenshots + network tab).
2. Choose AA stack (Alchemy Account Kit / permissionless.js / viem) targeting chain **4663**.
3. WebAuthn create → smart account factory deploy (or counterfactual address).
4. Gas sponsorship policy for first txs.
5. In-app “Blockchain → Wallet” sub-tab: create / restore passkey, show address, no seed phrase UX.
6. Security review + GrapheneOS-friendly storage (Android Keystore / platform authenticator).

---

## 3. Product shape (this release)

Desktop **Blockchain** tab (CustomTkinter):

1. Network selector default **Robinhood Chain (4663)**.
2. Section **RWA / Stock tokens** — ≥5 with price, 24h change if available, contract (short + copyable full).
3. Section **Top memecoins (DexScreener)** — top 10 by 24h volume on `robinhood`.
4. **Add contract** field for custom ERC-20 track (session list).
5. **Refresh** + status line (errors isolated per section).
6. **Self-test** button runs in-process checks (see §5).

Android: metadata bump only if we ship APK; primary UX is desktop toolkit for this phase (parity can follow).

---

## 4. Architecture

```text
php-usd-converter/
  blockchain/
    __init__.py
    networks.py          # Robinhood default + placeholders
    dexscreener.py       # HTTP client, retries, errors
    defaults.py          # 5 RWA contracts + meme filters
    tracker.py           # orchestrate RWA / meme / custom
    selftest.py          # extensive tests + try/except paths
  docs/
    ROBINHOOD_CHAIN_PLAN.md   # this file
  scripts/
    probe_blockchain.py       # CLI smoke before UI
  tools/browser-harness/      # local agent tool (repo root tools/)
```

---

## 5. Testing & resilience

- Every network call: timeout, HTTPError, JSON decode, empty pairs → structured `Result` / raised `BlockchainError` with user-safe message.
- Self-test covers: DexScreener search, RWA batch price, memecoin ranking length ≥10, contract format validation, network table integrity.
- CLI: `./venv/bin/python scripts/probe_blockchain.py`
- UI: “Run self-test” prints pass/fail lines in status box.

---

## 6. Out of scope (until you review this plan)

- Real passkey wallet creation
- Trading / swap
- hood.dev token launch from the app
- Robinhood brokerage login / Crypto Trading API keys
- Shipping browser-harness inside the mobile app

---

## 7. Decision log

| Decision | Choice |
|---|---|
| Default chain | Robinhood Chain (4663 / dexscreener `robinhood`) |
| Price source v1 | DexScreener only |
| RWA set | Top liquid stock-token tickers (NVDA, TSLA, AAPL, GOOGL, MSFT) with verified contracts |
| Memes | Live top 10 by h24 volume, exclude stables/stock symbols |
| Passkey | Documented for Phase C; research via browser-harness |
| PDF API | Documented; not required for chain tracker |
