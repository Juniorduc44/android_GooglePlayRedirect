# Turnkey SDK · Passkey login → Robinhood Chain wallet

**Status:** research complete · PoC scaffolded · blocked on your Turnkey org credentials  
**SDK clone:** `tools/passkey-research/tkhq-sdk` ([github.com/tkhq/sdk](https://github.com/tkhq/sdk))  
**PoC app:** `tools/passkey-research/rh-passkey-wallet/`  
**Date:** 2026-07-30  

---

## 1. What we cloned and what matters

Monorepo packages (focus list for passkey + EVM wallet):

| Package | Role |
|---|---|
| **`@turnkey/react-wallet-kit`** (v2.2.x) | **Primary path.** React hooks + auth modal. Auth Proxy = no backend required. Supersedes `sdk-react`. |
| **`@turnkey/core`** | Low-level client: `loginWithPasskey`, `signUpWithPasskey`, `createPasskey`, `createWallet`, sessions. |
| **`@turnkey/webauthn-stamper`** | Browser WebAuthn stamps Turnkey API requests (`X-Stamp-Webauthn`). |
| **`@turnkey/react-native-passkey-stamper`** | Mobile passkeys (Android Credential Manager / iOS). |
| **`@turnkey/react-native-wallet-kit`** | Full RN embedded wallet kit. |
| **`@turnkey/viem` / `@turnkey/ethers`** | Sign ETH txs with Turnkey-held keys (EOA on chain). |
| **`@turnkey/http`** | Typed HTTP client. |
| **`@turnkey/sdk-browser`** | **Deprecated** — use core / react-wallet-kit. |

**This is the same stack hood.dev ships** (we reverse-engineered: `@turnkey/core`, Auth Proxy, passkey stamper, `createSubOrganization` / `createWallet`).

---

## 2. Mental model (do not confuse with DIY WebAuthn wallets)

```
┌─────────────────────┐     stamp (WebAuthn)      ┌──────────────────────────┐
│  User device        │ ───────────────────────►  │  Turnkey secure enclave  │
│  Passkey (P-256)    │                           │  secp256k1 wallet keys   │
│  biometric / PIN    │ ◄──── session JWT ─────── │  never leave enclave     │
└─────────────────────┘                           └──────────────────────────┘
         │                                                    │
         │  UI: login / create wallet / sign                  │ broadcast
         ▼                                                    ▼
   Browser / WebView / RN                              Robinhood Chain 4663
```

- **Passkey ≠ ETH private key.** Passkey *authorizes* Turnkey to use enclave keys.
- Each user gets a **sub-organization** under your parent org.
- Parent org can **read** structure; policies prevent parent from spending user funds.
- Address is a normal **EOA** → works with RH RPC, DEX, Spec tab later.

---

## 3. Two implementation paths (pick one)

### Path A — Auth Proxy (recommended for v1 / hood.dev-like)

**No custom backend.** Dashboard enables passkeys + Auth Proxy.

```tsx
// TurnkeyProvider
{
  organizationId: "...",
  authProxyConfigId: "...",
  auth: {
    // enable passkey in dashboard + optional modal config
  },
  passkeyConfig: { rpId: "your.domain.com" }, // "localhost" in dev
}

// UI
const { handleLogin, loginWithPasskey, signUpWithPasskey, wallets, createWallet } = useTurnkey();
<button onClick={() => loginWithPasskey()}>Passkey</button>
// or full modal: handleLogin()
```

| Env var | Source |
|---|---|
| `NEXT_PUBLIC_ORGANIZATION_ID` | Turnkey Dashboard |
| `NEXT_PUBLIC_AUTH_PROXY_ID` | Dashboard → Auth / Auth Proxy |
| `NEXT_PUBLIC_RP_ID` | Domain for WebAuthn (localhost / your host) |

**Reference demos in clone:**

- `tkhq-sdk/examples/demos/with-react-wallet-kit/` — full Auth Proxy demo  
- Official docs: [React auth](https://docs.turnkey.com/solutions/embedded-wallets/integration-guide/react/auth)

### Path B — Your backend (more control / co-signing later)

Server holds parent **API keys**. Creates sub-orgs. Login still client-side passkey.

**Reference:** `tkhq-sdk/examples/authentication/with-passkeys/with-backend/`

Sign-up (1 passkey tap — SDK pattern):

1. Optional email lookup → new vs returning  
2. `createApiKeyPair()` temp P-256 in IndexedDB  
3. `createPasskey()` → attestation  
4. Backend `createSubOrganization` (passkey root + temp API key 60s)  
5. Client `stampLogin` with temp key → long session  
6. Delete temp key  

Login:

1. Lookup sub-org  
2. `loginWithPasskey()` → WebAuthn assertion → session  

---

## 4. Core API surface (from `packages/core`)

| Method | Purpose |
|---|---|
| `createPasskey({ name })` | WebAuthn registration ceremony |
| `signUpWithPasskey(...)` | Create passkey + sub-org + session (Auth Proxy helper) |
| `loginWithPasskey(...)` | Stamp login with existing passkey → session JWT |
| `createWallet({ walletName, accounts })` | New HD wallet in sub-org (ETH account) |
| `createWalletAccounts` | Extra addresses |
| Session store | IndexedDB / secure storage — non-extractable session keys |

**Stamping:** Every privileged request is signed either by:

- Passkey (`WebauthnStamper` / RN stamper), or  
- Session API key (after login), or  
- Parent API key (server only)

**Security property:** request bodies are hashed; passkey signs the challenge — phishing-resistant (bound to RP ID).

---

## 5. Robinhood Chain integration plan

| Step | Detail |
|---|---|
| Chain | id **4663**, gas ETH |
| RPC | public `https://rpc.mainnet.chain.robinhood.com` or Alchemy |
| Explorer | `https://robinhoodchain.blockscout.com` |
| Signer | `@turnkey/viem` `createAccount` + `createWalletClient` |
| Address format | `ADDRESS_FORMAT_ETHEREUM` on createWallet |
| Product UI | Passkey login → show address + balance → send (later) |

**viem sketch (post-auth):**

```ts
import { createAccount } from "@turnkey/viem";
import { createWalletClient, http, defineChain } from "viem";

const robinhood = defineChain({
  id: 4663,
  name: "Robinhood Chain",
  nativeCurrency: { name: "Ether", symbol: "ETH", decimals: 18 },
  rpcUrls: {
    default: { http: ["https://rpc.mainnet.chain.robinhood.com"] },
  },
});

const account = await createAccount({
  client: turnkeyHttpClient, // stamped session
  organizationId: userSubOrgId,
  signWith: walletAccountAddressOrId,
});

const walletClient = createWalletClient({
  account,
  chain: robinhood,
  transport: http(),
});
```

---

## 6. How this fits php-usd-converter

Native CustomTkinter **cannot** host WebAuthn. Android pure forms **cannot** without Credential Manager / WebView.

| Surface | Approach |
|---|---|
| **v1 PoC** | Standalone Next app `rh-passkey-wallet` (this folder) |
| **Desktop toolkit** | Open system browser / embed WebView to PoC origin |
| **Android** | Chrome Custom Tab or WebView + JS bridge; or RN kit later |
| **Later UX clone** | RainbowKit / MetaMask UX patterns *on top of* Turnkey (do not reimplement key storage) |

**Do not** reintroduce password-encrypted local private keys as the primary product — that was the wrong model vs hood.dev.

---

## 7. Security hardening roadmap

### Ship-blocking (v1 functional + safe baseline)

1. **HTTPS + correct RP ID** in production (never loose RP on public domains)  
2. **Passkeys only** (or passkey + OTP) for fund-moving auth  
3. **Auth Proxy / org policies:** deny parent org signing user txs  
4. **Session TTL** short + re-auth for sends  
5. **No API private keys in the client** (Path A: none; Path B: server only, env secrets)  
6. **CSP** on the wallet origin; isolate from converter origin if possible  
7. **Subresource integrity** / pin SDK versions  

### “Secure as heck” (v2)

| Layer | Action |
|---|---|
| Phishing | Dedicated wallet subdomain; fixed RP ID; no deep-link traps |
| Session | Non-extractable IndexedDB keys; clear on logout; device binding where available |
| Signing UX | Always show clear amount / to / chainId 4663 before stamp |
| Policies | Turnkey policies: max transfer, allowlist, time windows |
| Recovery | Second passkey or email OTP recovery *before* mainnet money |
| Export | Export only via Turnkey iframe; warn user; never log seeds |
| Mobile | Prefer RN passkey stamper or Custom Tabs over insecure WebView JS bridges |
| Audit | Threat model doc + dependency audit; optional pen-test before marketing “self-custody” |
| Cloning Rainbow/MM | Use for **UI/dapp connect** only; **keys stay Turnkey** |

### Threats Turnkey already reduces

- No seed phrase phishing surface for default path  
- Keys not in app memory  
- Passkey phishing resistance (origin bound)  

### Threats we still own

- XSS on our domain can request stamps while user is logged in  
- Malicious RP ID misconfig  
- Social recovery / account takeover of Google/Apple if they sync passkeys  
- User exports seed and is phished later  

---

## 8. Later: Rainbow / MetaMask “clone” strategy

| Goal | Reuse | Avoid reinventing |
|---|---|---|
| Pretty portfolio + send UI | Rainbow design patterns / open components | Keyring |
| dApp connect | WalletConnect + EIP-1193 (`@turnkey/eip-1193-provider`) | Injected `window.ethereum` from scratch |
| Token lists / swaps | Open APIs + RH DEX | Custodial backend |

**Order:** Passkey login + create ETH wallet + RH balance/send **first**. Then WC / dApp. Then polish to feel like Rainbow.

Relevant SDK pieces:

- `packages/eip-1193-provider`  
- Examples: `with-eip-1193-provider`, AA demos (optional)  

---

## 9. Implementation phases

### Phase P0 — Credentials (you)

1. Create org at [app.turnkey.com](https://app.turnkey.com)  
2. Enable **Passkeys** + **Auth Proxy**  
3. Note `ORGANIZATION_ID` + `AUTH_PROXY_ID`  
4. For local: RP ID `localhost`  

### Phase P1 — Passkey login PoC (in progress scaffold)

- [x] Clone SDK  
- [x] Write this plan  
- [x] Scaffold `rh-passkey-wallet` (Auth Proxy + passkey buttons + wallet list)  
- [ ] You fill `.env.local`  
- [ ] `pnpm install && pnpm dev` — one-tap passkey signup/login works  

### Phase P2 — Robinhood wallet actions

- createWallet ETH account if missing  
- balance on 4663  
- send ETH (passkey or session stamp)  
- explorer link  

### Phase P3 — Product integration

- Android WebView / Custom Tab entry from toolkit  
- Desktop “Wallet” menu opens secure origin  
- Copy: *self-custody via Turnkey · not Robinhood brokerage*  

### Phase P4 — Hardening + WC

- Policies, CSP, re-auth on send  
- WalletConnect optional  
- Recovery path  

---

## 10. PoC runbook (`rh-passkey-wallet`)

```bash
cd tools/passkey-research/rh-passkey-wallet
cp .env.local.example .env.local
# edit NEXT_PUBLIC_ORGANIZATION_ID, NEXT_PUBLIC_AUTH_PROXY_ID, NEXT_PUBLIC_RP_ID=localhost
pnpm install
pnpm dev
```

Open http://localhost:3000 → **Create / Sign in with Passkey**.

---

## 11. What you must provide before login works

Without these, code cannot complete a live ceremony:

1. Turnkey **organization ID**  
2. **Auth Proxy config ID** (Path A) **or** server API public/private keys (Path B)  
3. Deployment **domain** for production RP ID  

Reply with Path A vs B preference + credentials (or secrets file path) to proceed to live login.

---

## 12. Source map (local)

```
tools/passkey-research/
  tkhq-sdk/                          # full SDK monorepo
  demo-embedded-wallet/              # earlier Turnkey demo
  turnkey-demo-passkey-wallet/       # deprecated demo
  rh-passkey-wallet/                 # our RH-focused PoC
  TURNKEY_SDK_PASSKEY_PLAN.md        # this file
  PASSKEY_WALLET_RESEARCH.md         # earlier industry research
```

Key source files studied:

- `tkhq-sdk/packages/core/src/__clients__/core.ts` — `loginWithPasskey` / `signUpWithPasskey`  
- `tkhq-sdk/packages/webauthn-stamper/src/index.ts` — stamp construction  
- `tkhq-sdk/packages/react-wallet-kit/src/providers/client/Provider.tsx` — hooks  
- `tkhq-sdk/examples/authentication/with-passkeys/with-backend/` — 1-tap signup pattern  
- `tkhq-sdk/examples/demos/with-react-wallet-kit/` — Auth Proxy reference  
