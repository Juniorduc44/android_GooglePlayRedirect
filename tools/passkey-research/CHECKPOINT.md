# Checkpoint — Passkey / Turnkey wallet research (paused)

**Date:** 2026-07-31  
**Status:** **PARKED** — unfinished wallet UI must not ship in the main app.  
**Main product:** `php-usd-converter` **v1.8.1** (Spec + Chain viewer, **no Wallet tab**).

---

## Product decision (now)

| Surface | State |
|---|---|
| Desktop / Android **Wallet** menu | **Removed** (do not re-add until passkey flow works end-to-end) |
| Chain | Markets viewer only |
| Spec | Price calculators + Dex import |
| Turnkey passkey PoC | Research only under `tools/passkey-research/` — **not** in APK |

---

## Where research lives

| Path | What |
|---|---|
| `TURNKEY_DOCS_AND_CHECKLIST.md` | Master checklist + docs summary |
| `TURNKEY_SDK_PASSKEY_PLAN.md` | Implementation architecture |
| `PASSKEY_WALLET_RESEARCH.md` | Earlier industry / hood.dev research |
| `rh-passkey-wallet/` | Next.js PoC (Auth Proxy + handleLogin + CLI backend scaffolding) |
| `rh-passkey-wallet/BACKEND.md` | Stamped API + CLI notes |
| `turnkey-docs/` | Offline extracts of Turnkey docs + whitepaper |
| `CHECKPOINT.md` | **This file** |

### Not committed (re-clone when resuming)

Large local clones (optional; re-fetch from GitHub):

```bash
cd tools/passkey-research
git clone --depth 1 https://github.com/tkhq/sdk.git tkhq-sdk
# also previously used: daimo-p256-verifier, passkeys-4337-smart-wallet, w3pk, base-webauthn-sol, turnkey demos
```

---

## Resume checklist (later)

### A. Credentials (you)

- [x] Turnkey org account  
- [x] `NEXT_PUBLIC_ORGANIZATION_ID`  
- [x] `NEXT_PUBLIC_AUTH_PROXY_ID` (Wallet Kit / Auth Proxy config)  
- [x] Allowed origin `http://localhost:3456` (confirm still set)  
- [ ] **`API_PUBLIC_KEY` + `API_PRIVATE_KEY`** (parent org API key) — **required for CLI stamped whoami / create-suborg**  
- [ ] Confirm Passkeys **enabled** in Auth Proxy config  

Put secrets only in `rh-passkey-wallet/.env.local` (gitignored). Never commit.

### B. Run PoC

```bash
cd tools/passkey-research/rh-passkey-wallet
cp .env.local.example .env.local   # if needed; fill IDs + API keys
npm install
npm run dev                        # http://localhost:3456
npm run tk:health                  # needs API keys → expect Stamp path OK
```

### C. Known issues at pause

1. **Auth Proxy alone ≠ server stamps** — CLI/backend needs parent API keys.  
2. **WebAuthn only after user gesture** — use official `handleLogin()`; do not call `loginWithPasskey` on page load.  
3. **Passkey create vs get** — kit modal: Sign up = create credential, Log in = get; product-wise same account model.  
4. **Allowed Origins** must match exact origin (e.g. `http://localhost:3456`).  
5. Main app must stay **wallet-free** until flow is solid.

### D. After PoC works

1. Hardening (CSP, RP ID, session policies).  
2. Optional Android Custom Tab / WebView to PoC.  
3. Only then re-introduce a **Wallet** section in php-usd-converter.  
4. Rainbow/MetaMask-style UI later — keys stay Turnkey.

---

## How this checkpoint was saved

- Git commit on `main` with research docs + `rh-passkey-wallet` source (no secrets, no `node_modules`).  
- Tag: see `git tag -l 'checkpoint*'` after push.  
- App release remains **v1.8.1** (no unfinished Wallet UI).

---

## Do not

- Ship unfinished Wallet tab  
- Commit `.env.local` or API private keys  
- Treat Auth Proxy Config ID as a substitute for API key stamps on the server  
