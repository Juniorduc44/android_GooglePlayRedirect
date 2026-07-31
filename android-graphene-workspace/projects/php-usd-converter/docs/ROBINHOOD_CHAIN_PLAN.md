# Plan: Robinhood Chain access, price tracker & passkey wallet

**Status:** Phase 1 (price tracker) + Spec are the product. **Wallet / passkey parked** (2026-07-31) — no unfinished Wallet UI in the app.  
**Passkey research checkpoint:** `tools/passkey-research/CHECKPOINT.md`  
**UI + speed next steps:** `docs/UI_PERF_PLAN.md`  
**Date:** 2026-07-31  
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

### 1.3 hood.dev /launch (passkey / wallet research) — **completed 2026-07-29**

Observed public product:

- **hood.dev** is a **token launchpad** on Robinhood Chain (“Launch, trade, and scale tokens”).
- **/launch** creates an ERC-20 + Uniswap/Sushi V3 pool with LP locked, anti-sniper caps, deploy cost ~0.001 ETH.
- Wallet UX is **Connect** with **Turnkey-backed passkey/session** (not open source).

**Verified stack (production JS reverse-engineering):**

- `https://api.turnkey.com`, `https://authproxy.turnkey.com`
- `@turnkey/core@2.2.0`, PasskeyStamper, `createSubOrganization`, `createWallet`
- WalletConnect + React wallet kit hooks (`useTurnkeyWallets`)

**Full write-up + architecture options (Turnkey / w3pk-PRF / ERC-4337 P-256):**

→ `tools/passkey-research/PASSKEY_WALLET_RESEARCH.md`  
→ Clones: `tools/passkey-research/{turnkey-demo-passkey-wallet,demo-embedded-wallet,passkeys-4337-smart-wallet,daimo-p256-verifier,base-webauthn-sol,w3pk,portkey-client}`

**Status:** Research complete. Password EOA Wallet tab is **not** the passkey product. Implementation blocked on path choice + (for hood-identical UX) Turnkey org credentials. See research §6–§8.

**Legal / product caution:** Do not claim “official Robinhood login” or clone trademarks. Build **compatible** EVM + passkey UX for RH chain; passkey is **user self-custody**, not RH brokerage auth.

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

### Phase C — passkey smart wallet (plan status)

#### Is the plan complete?

| Piece | Status |
|---|---|
| Chain identity (4663, RPC, explorer, AA support) | **Done** (docs + app network card) |
| Price / market data path | **Done** (DexScreener) |
| hood.dev public product mapped | **Partial** — see below |
| Exact hood.dev “passkey → ERC-20 wallet in ms” reverse-engineer | **Not complete** |
| App wallet UI / signing | **Not started** |
| Legal/product framing | **Drafted** (self-custody, not RH brokerage login) |

#### What hood.dev actually is (re-checked)

- **hood.dev /launch** is a **token launchpad** on Robinhood Chain: name/symbol → deploy ERC-20 + Uniswap/Sushi V3 pool, LP locked, anti-sniper caps.
- Public UI says **“Connect a wallet to generate your address”** — classic wallet-connect / injected EVM wallet, **not** a documented open “clone this passkey API” product.
- We did **not** obtain: their smart-account factory address, WebAuthn contract, bundler URL, or paymaster policy. Those require browser-harness session capture against a live login (and may be closed-source / proprietary).

#### Is passkey-style login **possible** in our app?

**Yes, in principle — as our own wallet UX on Robinhood Chain, not by embedding hood.dev’s private login.**

Feasible architecture (industry-standard, works on chain 4663 because RH documents **ERC-4337**):

1. **WebAuthn / platform passkey** (Android Credential Manager / browser `navigator.credentials`) creates a P-256 key the user never sees.
2. That key owns an **ERC-4337 smart account** on RH Chain (Alchemy Account Kit, ZeroDev, Biconomy, or permissionless.js + a factory).
3. **Bundler + optional paymaster** (Alchemy gasless) so first txs feel “milliseconds / no gas UX.”
4. App stores only: account address + credential id handle (Android Keystore / encrypted prefs). **No seed phrase** in the happy path.
5. GrapheneOS: prefer hardware-backed keys; no GMS-required APIs if we stick to AndroidX Credential Manager + standard WebAuthn.

**Not feasible / not recommended:**

- Calling hood.dev’s internal passkey endpoints without their permission/API (brittle, ToS, may break anytime).
- Claiming “Robinhood login” or reusing their branding for auth.
- Shipping a full wallet without security review, recovery UX, and clear “self-custody / not RH broker” copy.

#### Recommended next steps when you green-light Phase C

1. browser-harness session on hood.dev: capture whether they use WalletConnect, injected `window.ethereum`, or a true WebAuthn popup.
2. Spike Alchemy Account Kit on chain **4663** (create SA, one sponsored transfer, one Uniswap read).
3. Desktop-first “Wallet” panel under Chain: Create passkey wallet · Show address · Copy · (later) sign typed data.
4. Android second: Credential Manager + same AA backend.
5. Only then consider hood.dev launch *integration* as “open external launchpad with our address,” not clone.

**Bottom line:** Plan for **data** is complete and shipped. Plan for **passkey wallet** is directionally complete and **technically possible** on RH Chain via AA + WebAuthn; it is **not** “copy hood.dev’s login binary.” Implementation is still Phase C work, not in the app yet.

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
