# Turnkey documentation pack + master checklist

**For:** php-usd-converter / Robinhood Chain passkey wallet  
**Date:** 2026-07-30  
**Your situation:** You have a Turnkey **account**. You do **not** need to invent secrets for me yet — you need to **copy two public IDs** from the dashboard (safe to paste). Private API keys stay only on a server, never in chat if avoidable.

---

## What you should give me (when ready)

From [app.turnkey.com](https://app.turnkey.com) dashboard:

| Item | Where | Safe to share? |
|---|---|---|
| **Organization ID** | Dashboard home / org settings (looks like a long hex or UUID) | **Yes** (public-ish config) |
| **Auth Proxy Config ID** | **Embedded Wallets → Configuration** after enabling Auth Proxy | **Yes** |
| Allowed origin | e.g. `http://localhost:3456` | Yes |
| Parent **API private key** | API keys section | **No in chat** — put in local `.env` only if we use Path B backend |

**Minimum for Path A (recommended):** Organization ID + Auth Proxy Config ID.

---

# Master checklist

Legend: **[x] done** · **[ ] not done** · **[~] partial**

## A. Documentation & research (agent)

- [x] Cloned Turnkey SDK → `tools/passkey-research/tkhq-sdk`
- [x] Wrote SDK passkey plan → `TURNKEY_SDK_PASSKEY_PLAN.md`
- [x] Scaffolded RH PoC app → `rh-passkey-wallet/`
- [x] Fetched API overview (RPC/POST-only/stamps)
- [x] Fetched Stamps docs (API key stamp + WebAuthn stamp)
- [x] Fetched Auth Proxy docs
- [x] Fetched Passkey integration docs
- [x] Fetched Sub-organizations docs
- [x] Fetched Wallets + Ethereum network docs
- [x] Fetched React Embedded Wallet Kit getting-started
- [x] Fetched Sessions docs
- [x] Downloaded **Whitepaper** (multi-chapter text) → `turnkey-docs/whitepaper/`
- [x] Saved key doc pages as local markdown → `turnkey-docs/`
- [x] Indexed full docs catalog via `https://docs.turnkey.com/llms.txt`
- [ ] Live passkey ceremony tested with **your** org credentials
- [ ] Production domain / HTTPS RP ID configured

## B. Your Turnkey dashboard (you do this)

Do these in order. Copy IDs into `.env.local` when done.

### B1. Account & org

- [ ] Logged into [app.turnkey.com](https://app.turnkey.com)
- [ ] Organization created (if signup only made a user, finish org setup)
- [ ] **Organization ID** copied
- [ ] Root user secured (you understand root quorum controls the org)

### B2. Auth Proxy (Path A — easiest)

- [ ] Open **Embedded Wallets → Configuration**
- [ ] **Auth Proxy** toggle **ON**
- [ ] Enable **Passkeys** (required)
- [ ] Optional: enable Email OTP (backup onboarding)
- [ ] **Save** settings
- [ ] Copy **Auth Proxy Config ID**
- [ ] Set **Allowed Origins** to exact URLs:
  - [ ] `http://localhost:3456` (our PoC default port)
  - [ ] (later) production site, e.g. `https://wallet.yourdomain.com`
- [ ] Note session expiration (default often **900s** = 15 min — fine for PoC)

### B3. Passkey / RP ID notes

- [ ] Understand RP ID must match your site:
  - local: `localhost`
  - prod: e.g. `yourdomain.com` (not full path)
- [ ] Passkeys tested on a real browser (Chrome/Safari) with platform authenticator

### B4. Optional later (not needed for first login)

- [ ] Create server API key (Path B only) — store private key offline
- [ ] IP allowlist for API keys
- [ ] OAuth (Google/Apple) — not required for passkey-only
- [ ] Policies for max transfer / allowlist destinations
- [ ] Webhooks for balance / activity

## C. Local PoC wiring

- [ ] `cd tools/passkey-research/rh-passkey-wallet`
- [ ] `cp .env.local.example .env.local`
- [ ] Fill:
  - [ ] `NEXT_PUBLIC_ORGANIZATION_ID=...`
  - [ ] `NEXT_PUBLIC_AUTH_PROXY_ID=...`  *(same as Auth Proxy Config ID)*
  - [ ] `NEXT_PUBLIC_RP_ID=localhost`
- [ ] `pnpm install` (or `npm install`)
- [ ] `pnpm dev` → http://localhost:3456
- [ ] Browser: **Create wallet with Passkey** succeeds
- [ ] Browser: **Sign in with Passkey** works after reload
- [ ] ETH address appears (or **Create ETH wallet** works)
- [ ] Balance query on Robinhood Chain **4663** works (may be 0)

## D. Product features (after login works)

- [ ] Create ETH wallet on signup automatically
- [ ] Show RH balance + explorer link
- [ ] Send ETH on 4663 (passkey or session stamp + confirm UI)
- [ ] Sign message (SIWE / raw) for testing
- [ ] Logout clears session
- [ ] Desktop toolkit opens PoC (browser or WebView)
- [ ] Android Custom Tab / WebView entry
- [ ] Remove any leftover password-keystore UX as primary path

## E. Security hardening (before real money)

- [ ] HTTPS production host + correct RP ID
- [ ] Auth Proxy allowed origins = production only (not `*`)
- [ ] No API private keys in frontend or APK
- [ ] Short session TTL + re-auth on send
- [ ] Turnkey policies: parent cannot spend user funds (sub-org model)
- [ ] Clear “to / amount / chain 4663” confirmation before stamp
- [ ] CSP on wallet origin
- [ ] Recovery path (2nd passkey or OTP) documented for users
- [ ] Export flow only via Turnkey iframe; user warnings
- [ ] Threat model written for XSS / phishing / device loss

## F. Later UX (Rainbow / MetaMask-class)

- [ ] Research WalletConnect + `@turnkey/eip-1193-provider`
- [ ] Portfolio UI patterns (not reimplement keyring)
- [ ] Optional AA (4337) only if we need smart-account features

---

# What the docs say (condensed)

## 1. API model ([intro](https://docs.turnkey.com/api-reference/overview/intro))

- Turnkey API is **RPC over HTTP**.
- Almost everything is **POST**.
- Every request must be **stamped** (cryptographic signature over the body).
- Two kinds of calls:
  - **Queries** — reads (`list_wallets`, `whoami`, …)
  - **Submissions / Activities** — writes (`create_wallet`, `sign_transaction`, …)

Local copy: `turnkey-docs/api-reference_overview_intro.md`

## 2. Stamps ([stamps](https://docs.turnkey.com/api-reference/overview/stamps))

| Kind | Header | Who signs |
|---|---|---|
| API key | `X-Stamp` (base64url JSON) | Server or session API key |
| WebAuthn / passkey | `X-Stamp-Webauthn` (JSON) | User device passkey |

Passkey stamp includes: `credentialId`, `authenticatorData`, `clientDataJson`, `signature`.  
Challenge = SHA-256 of POST body (as hex string, then encoded for WebAuthn).

SDK stampers do this for us: `@turnkey/webauthn-stamper`, `@turnkey/api-key-stamper`, etc.

Local copy: `turnkey-docs/api-reference_overview_stamps.md`

## 3. Auth Proxy ([auth-proxy](https://docs.turnkey.com/features/authentication/auth-proxy))

- Host: `https://authproxy.turnkey.com`
- Signs **signup / OTP / OAuth / account lookup** so **you don’t need a backend** for auth.
- **Cannot** move funds by itself; user still must participate (passkey / OTP / OAuth).
- Header: `X-Auth-Proxy-Config-Id: <your config id>`
- Dashboard: enable proxy, set origins, enable passkeys.

Auth Proxy endpoints (high level):

| Endpoint purpose | Path (v1) |
|---|---|
| Wallet kit config | `/v1/wallet_kit_config` |
| Account lookup | `/v1/account` |
| Signup (create sub-org) | `/v1/signup_v2` |
| OTP init / verify / login | `/v1/otp_*` |
| OAuth login | `/v1/oauth_login` |

Local copy: `turnkey-docs/features_authentication_auth-proxy.md`

## 4. Passkeys ([integration](https://docs.turnkey.com/features/authentication/passkeys/integration))

1. Frontend triggers WebAuthn (`create` or `get`)
2. Device produces signature
3. Optional: proxy via your backend (not required)
4. Turnkey **enclave** verifies stamp

Registration → attach passkey as **authenticator** on user/sub-org.  
Authentication → stamp login / activities with passkey.

Local copy: `turnkey-docs/features_authentication_passkeys_integration.md`

## 5. Sub-organizations ([docs](https://docs.turnkey.com/features/sub-organizations))

- **One sub-org per end user** (typical embedded wallet).
- Parent has **read-only** visibility.
- End user (passkey root) owns keys; parent does not freely sign for them.
- Optional wallet created at sub-org creation time.

This is the non-custodial model hood.dev-style products use.

Local copy: `turnkey-docs/features_sub-organizations.md`

## 6. Wallets ([docs](https://docs.turnkey.com/features/wallets))

- HD wallets + accounts (ETH address format for us).
- Create / list / export / import via activities.
- Export uses protected iframe flows in product SDKs.

Local copy: `turnkey-docs/features_wallets.md`

## 7. Ethereum / EVM ([docs](https://docs.turnkey.com/features/networks/ethereum))

- Turnkey is **curve-level** (secp256k1) → works on any EVM including **Robinhood Chain 4663**.
- Sign transaction / raw payload; broadcast yourself via RPC (or Turnkey tx management if enabled).

Local copy: `turnkey-docs/features_networks_ethereum.md`

## 8. React Embedded Wallet Kit setup ([getting started](https://docs.turnkey.com/solutions/embedded-wallets/integration-guide/react/getting-started))

Exact dashboard path for you:

1. Enable **Auth Proxy**  
2. Enable **passkeys** (and optional email OTP)  
3. Save → copy **Auth Proxy Config ID** + **Organization ID**  
4. Env:
   ```bash
   NEXT_PUBLIC_ORGANIZATION_ID="..."
   NEXT_PUBLIC_AUTH_PROXY_CONFIG_ID="..."   # our PoC also uses NEXT_PUBLIC_AUTH_PROXY_ID
   ```
5. Wrap app in `TurnkeyProvider`
6. Wait for `ClientState.Ready` before calling auth methods
7. `loginWithPasskey` / `signUpWithPasskey` / `handleLogin`

Local copy: `turnkey-docs/solutions_embedded-wallets_integration-guide_react_getting-started.md`

## 9. Whitepaper ([whitepaper.turnkey.com](https://whitepaper.turnkey.com/))

Downloaded as local text chapters (HTML → markdown extract; **not** a single official PDF — site is multi-page web whitepaper):

| File | Topic |
|---|---|
| `turnkey-docs/whitepaper/index.md` | Overview |
| `turnkey-docs/whitepaper/principles.md` | Key management from first principles |
| `turnkey-docs/whitepaper/foundations.md` | Verifiable foundations, QuorumOS, attestation |
| `turnkey-docs/whitepaper/architecture.md` | Enclave architecture |
| `turnkey-docs/whitepaper/applications.md` | Applications beyond pure KM |

**Core claim (summary):** keys and policy evaluation live in **secure enclaves** with **remote attestation**; operators cannot silently rewrite enclave code; parent infrastructure cannot freely mint user signatures without policy + user credentials.

Docs index page: https://docs.turnkey.com/security/whitepaper  

---

# How this maps to our project

```
User phone/laptop passkey
        │
        ▼
rh-passkey-wallet (Next)  ──Auth Proxy──►  Turnkey
        │                                     │
        │  createWallet / sign                │ enclave secp256k1
        ▼                                     ▼
  show address + balance              Robinhood Chain 4663 RPC
```

**Already built in repo:**

- Research + SDK clone  
- PoC scaffold expecting your two IDs  
- Spec / Chain markets (separate product surfaces)  

**Blocked only on:** your dashboard checklist **B** → then **C**.

---

# Folder map

```
tools/passkey-research/
  TURNKEY_DOCS_AND_CHECKLIST.md     ← this file
  TURNKEY_SDK_PASSKEY_PLAN.md       ← implementation architecture
  PASSKEY_WALLET_RESEARCH.md        ← earlier industry research
  tkhq-sdk/                         ← github.com/tkhq/sdk
  rh-passkey-wallet/                ← PoC app
  turnkey-docs/                     ← downloaded docs pages
    whitepaper/                     ← whitepaper chapters
```

---

# Immediate next action for you

1. Open [app.turnkey.com](https://app.turnkey.com)  
2. Complete **section B** checkboxes above  
3. Paste back **only**:
   - Organization ID  
   - Auth Proxy Config ID  
   - Confirm origins include `http://localhost:3456`  

Then I will mark B/C items done, fill env, run the PoC, and walk passkey create/login live.

You do **not** need to send API private keys for the Auth Proxy path.
