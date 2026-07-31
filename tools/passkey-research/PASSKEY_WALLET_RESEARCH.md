# Passkey → Wallet Research Report

**Status:** research complete (2026-07-29)  
**Product goal:** hood.dev-style **one-button passkey → usable EVM wallet** (no seed paste, no private-key form clutter) for Robinhood Chain (4663).  
**Verdict (short):** The shipped password/EOA Wallet tab is **not** that product. hood.dev is **Turnkey-backed**, not “derive address from passkey public key.” A true one-tap passkey wallet is **implementable only with one of the architectures below**; pure CustomTkinter / plain Android forms **cannot** host WebAuthn alone.

---

## 0. Direct answer to “what happened to the research?”

Earlier work shipped a **self-custody EOA keystore** (password + create/import private key) as a stopgap after markets/nav. That was the wrong product surface for the passkey goal. This document is the completed research: live hood.dev JS reverse-engineering, seven local GitHub clones, and architecture comparison against industry docs.

**We will not claim “passkey wallet done” until a path from §6 is implemented and demoed.**  
**We will not ship more private-key entry boxes as the passkey solution.**

---

## 1. Fact-check of the AI reply you shared

That reply describes a **plausible generic** “identity-to-wallet binding” pattern. Parts are industry-true; **several claims do not match hood.dev or real WebAuthn math.**

| Claim in the AI reply | Reality |
|---|---|
| “Backend derives wallet address from the passkey’s public key” | **Usually false for EOAs.** WebAuthn keys are **P-256 (secp256r1)**. Ethereum EOAs need **secp256k1**. You cannot treat the passkey pubkey as an ETH address without a **smart account**, **TEE wallet service**, or **PRF/encrypted secp256k1** bridge. |
| “Private key never exposed — signing via WebAuthn” | **Only true for P-256 path** (ERC-4337 smart account verifies WebAuthn). For classic EOAs, something else signs with secp256k1 (Turnkey enclave, or decrypted local key). |
| hood.dev uses SimpleWebAuthn + custom hash→address | **Not what we found.** Live hood.dev bundles are **Turnkey** (`@turnkey/core`, Auth Proxy, passkey stamper, `createSubOrganization`, `createWallet`). |
| “No public hood.dev source” | **Correct.** No public monorepo; we reverse-engineered production JS. |
| Libraries: SimpleWebAuthn | Common for **OSS** demos; **hood.dev uses Turnkey SDK + WalletConnect + viem-style stack**, not a pure SimpleWebAuthn DIY. |

**Correct mental model for “smooth passkey wallets”:**

```
Passkey (P-256, device enclave)
        │
        ├── Path A: AUTH ONLY → unlocks TEE/HSM wallet (Turnkey, etc.)
        │            secp256k1 key lives in enclave; passkey stamps API requests
        │
        ├── Path B: AUTH + ENCRYPT → unlocks local encrypted mnemonic (w3pk, Portkey PRF)
        │            secp256k1 key on device; AES key from WebAuthn metadata/PRF
        │
        └── Path C: SIGNER ON-CHAIN → smart account validates P-256 WebAuthn (4337)
                     address is contract AA, not an EOA from the passkey
```

The AI reply collapses these into one incorrect “derive EOA from passkey pubkey” story.

---

## 2. What hood.dev actually runs (verified 2026-07-29)

### 2.1 Public product

- Site: https://hood.dev (Next.js / Vercel `dpl_…`)
- Robinhood Chain token launchpad + trade UI
- Wallet UX: **Connect** → passkey/session restore → wallet pill with ETH balance + address
- Not open source

### 2.2 Evidence from production JS (downloaded homepage chunks)

Distinctive strings found in `/_next/static/chunks/*.js`:

| Marker | Meaning |
|---|---|
| `https://api.turnkey.com` | Turnkey API |
| `https://authproxy.turnkey.com` | Turnkey Auth Proxy (`/v1/wallet_kit_config`) |
| `https://export.turnkey.com`, `https://import.turnkey.com` | Turnkey key export/import iframes |
| `@turnkey/core@2.2.0` | Turnkey client SDK version baked in |
| `CREATE_PASSKEY_ERROR`, `PASSKEY_SIGNUP_AUTH_ERROR`, `PASSKEY_LOGIN_AUTH_ERROR` | Turnkey error codes |
| `INITIALIZE_PASSKEY_STAMPER_ERROR`, `PasskeyStamper` | Passkey stamps Turnkey requests |
| `CREATE_SUB_ORGANIZATION_ERROR`, `createSubOrganization` | Per-user Turnkey sub-org |
| `CREATE_WALLET_ERROR`, `createWallet` | Wallet creation inside sub-org |
| `useTurnkeyWallets`, session restore hooks | React wallet kit integration |
| WalletConnect cancel/expired errors | Secondary connect path |

**hood.dev flow (inferred from SDK + Turnkey docs/demo, consistent with bundle):**

1. User taps Connect / passkey.
2. Browser WebAuthn create/get (platform authenticator).
3. Turnkey **Auth Proxy** + **API**: create or resume **sub-organization** bound to passkey.
4. **createWallet** inside that sub-org → EVM address (secp256k1 material held in **Turnkey Nitro enclaves**, not in app plaintext).
5. Session persists; UI shows wallet instantly; signing goes through Turnkey with passkey stamp.

This is **embedded wallet infrastructure**, not “hash(P-256 pubkey) → 0x address.”

### 2.3 Why it feels “milliseconds / flawless”

- No seed phrase UI
- Native OS passkey sheet (Face ID / fingerprint / PIN)
- Wallet already exists after signup (sub-org + wallet atomic in product sense)
- Session restore without retyping secrets
- Signing happens remotely in enclave after local passkey authorization

---

## 3. Local research clones (GitHub playground)

All under `tools/passkey-research/`:

| Clone | Role | Key lesson |
|---|---|---|
| `turnkey-demo-passkey-wallet` (tkhq, deprecated) | Passkey reg/login → Turnkey sub-org → sign tx | Same pattern as hood.dev; needs org API keys + Go backend |
| `demo-embedded-wallet` (tkhq, current) | Auth Proxy OTP + passkey + OAuth + wallets | Production-shaped Turnkey kit (`@turnkey/react-wallet-kit`) |
| `passkeys-4337-smart-wallet` | WebAuthn P-256 → ERC-4337 SimpleAccount | **True** passkey-as-onchain-signer; needs bundler + factory deploy |
| `daimo-p256-verifier` | On-chain P-256 verify (RIP-7212 / precompile path) | Building block for Path C |
| `base-webauthn-sol` | Coinbase WebAuthn Solidity lib | Production-grade WebAuthn verification helper |
| `w3pk` | Browser SDK: passkey gates encrypted BIP39 wallet | Path B; **not** pure CustomTkinter; needs browser/WebView |
| `portkey-client` | PRF extension encrypts keys in cross-origin vault iframe | Path B variant; browser + own vault origin |

---

## 4. Architecture deep-dives

### 4.1 Path A — Turnkey / embedded wallet (hood.dev class)

**How keys work**

- User credential = passkey (or OTP/OAuth)  
- Signing key = secp256k1 inside **AWS Nitro enclave** (Turnkey)  
- Passkey **stamps** API requests; enclave signs txs only when stamp verifies  
- Parent org **cannot** freely spend user sub-org keys (policy model)

**Requirements to ship**

| Need | Notes |
|---|---|
| Turnkey organization | Paid/signup at app.turnkey.com |
| `ORGANIZATION_ID`, Auth Proxy ID, API keys | Server secrets |
| Frontend with WebAuthn | Browser, Chrome Custom Tab, or RN passkey stamper |
| Backend or Auth Proxy | Auth Proxy reduces custom backend for OTP/passkey |
| Domain / RP ID | Passkeys bound to relying party |

**Pros:** Closest UX match to hood.dev; EOA addresses work with normal RPC/DEX.  
**Cons:** Third-party infra dependency; needs accounts/keys we do not have in-repo; not fully OSS self-host.

**Android:** Turnkey publishes passkey stamper packages (`@turnkey/react-native-passkey-stamper` appears in hood bundles). Pure Kotlin without their SDK is possible via WebView/Custom Tabs + Auth Proxy, still needs org config.

**Desktop Tkinter:** **No native WebAuthn.** Must open system browser or embed WebView for the ceremony.

### 4.2 Path B — Local encrypted wallet + passkey unlock (w3pk / Portkey)

**How keys work (w3pk, from their ARCHITECTURE.md + `crypto.ts`)**

- WebAuthn **lock** (P-256) is separate from **wallet** (secp256k1 BIP39)
- Bridge: derive AES key from credential metadata and/or **WebAuthn PRF** extension
- Mnemonic encrypted at rest (IndexedDB); biometrics required to decrypt/sign
- Optional PRF session keys (HKDF from 32-byte PRF output)

**Portkey:** private key only decryptable via **PRF** inside sandboxed cross-origin iframe vault.

**Pros:** No Turnkey bill; self-custody on device; one-button register/login possible in **browser**.  
**Cons:** Needs browser APIs; PRF support varies by platform; recovery is harder than Turnkey policies; not “passkey signs the chain” — passkey unlocks a normal key.

**Android native:** Possible with **Credential Manager** + encrypted SharedPreferences/Keystore for the secp256k1 material, but that is a **custom port**, not drop-in w3pk (JS). GrapheneOS / device biometrics available via AndroidX Biometric + Credential Manager for passkeys.

**Desktop Tkinter:** Still needs browser/WebView for real WebAuthn.

### 4.3 Path C — ERC-4337 smart account owned by passkey (passkeys-4337 + Daimo)

**How keys work**

1. `navigator.credentials.create` → P-256 pubkey (x, y) + credential id  
2. Smart account address = CREATE2 from pubkey (deterministic)  
3. UserOps signed with WebAuthn assertion  
4. On-chain `WebAuthn` + P-256 verifier checks signature  
5. Bundler submits to EntryPoint  

**Cloned contracts:** `SimpleAccount.sol` holds `PublicKey { X, Y }`, validates via WebAuthn packing.

**Pros:** Cryptographically clean “passkey *is* the wallet controller”; no HSM vendor.  
**Cons:** Not an EOA — DEX/RPC tooling must speak AA; needs EntryPoint + factory on **Robinhood Chain 4663** (or chain support for P-256 precompile / verifier); bundler + gas sponsorship; more infra than a keystore; Robinhood docs advertise 4337 but we have **not** verified a deployed passkey factory on 4663 in this pass.

**Desktop/Android:** Signing still needs WebAuthn host (browser/WebView/Credential Manager).

### 4.4 Curve reality (why “derive EOA from passkey pubkey” is wrong)

| System | Curve | Signs ETH txs? |
|---|---|---|
| WebAuthn passkey | P-256 (secp256r1) | Not as EOA |
| Ethereum EOA | secp256k1 | Yes |
| 4337 + WebAuthn validator | P-256 verified in contract | Yes (as smart account) |
| Turnkey enclave wallet | secp256k1 inside TEE | Yes (as EOA) |
| w3pk STANDARD mode | secp256k1 after AES unlock | Yes (as EOA) |

---

## 5. Platform constraints for *this* repo

| Surface | WebAuthn / Passkeys? | Implication |
|---|---|---|
| `app.py` CustomTkinter | **No** | Cannot show a real passkey button without browser/WebView |
| Android `MainActivity` forms | **Indirect** | Credential Manager / WebView required; current UI is password EOA only |
| In-app WebView / Chrome Custom Tab | **Yes** (with caveats) | Best host for Path A/B/C ceremonies |
| Pure Python CLI | **No** | Research only |

Current shipped code (honest labels already in UI):

- Desktop: “This is NOT … hood.dev's passkey UI yet”
- Android `LocalWalletStore`: “not passkey AA yet”

That EOA keystore is a **foundation for Path B fallback**, not the product goal.

---

## 6. Implementation verdict

### 6.1 Can we build hood.dev-identical UX in this monorepo *today*?

| Path | Can we? | Blocker |
|---|---|---|
| **A Turnkey** | **Yes, after credentials** | Need Turnkey org + Auth Proxy config + domain RP ID. Code pattern fully available in `demo-embedded-wallet`. No org secrets in this repo → **cannot finish production wiring without you provisioning Turnkey**. |
| **B w3pk/PRF local** | **Yes for web/WebView** | Implementable without vendor; needs HTML/JS (or Kotlin Credential Manager + crypto) surface. **Not** pure Tkinter. |
| **C 4337 P-256** | **Yes as research/PoC** | Need bundler, factory deploy on 4663 (or testnet first), gas policy. Larger lift; best long-term OSS self-host. |
| **Derive EOA from passkey pubkey only** | **No (incorrect crypto)** | Would produce wrong security model / invalid EOA story. |

### 6.2 Explicit “cannot” statements

1. **Cannot** truthfully call the current password + private-key form a “passkey wallet.”  
2. **Cannot** implement real WebAuthn **inside CustomTkinter alone**.  
3. **Cannot** finish a **hood.dev clone** without either **Turnkey (or similar WaaS)** credentials **or** a full OSS Path B/C redesign with a browser/WebView host.  
4. **Cannot** use passkey P-256 public key bytes as a normal Ethereum private key / address without Path B or C machinery.

### 6.3 Recommended product path for php-usd-converter

**Recommended primary:** **Path A (Turnkey)** if the bar is “feels like hood.dev.”  
**Recommended OSS fallback:** **Path B in an Android WebView (or Chrome Custom Tab) mini-app** for passkey create/login + encrypted EOA, with Kotlin only displaying address/balance/send after JS bridge returns the address — **one primary button**, advanced import collapsed.

**Phase gate:** No more Wallet “complete” releases until:

1. User picks Path A vs B vs C (or provides Turnkey org), and  
2. UI is redesigned around **Create / Sign in with Passkey** (hide PK fields under “Advanced”).

---

## 7. What a correct UI looks like (target, not shipped)

```
┌─────────────────────────────────────┐
│  Wallet · Robinhood Chain (4663)    │
│                                     │
│  [  Create with Passkey  ]          │  ← only primary CTA if no wallet
│  [  Sign in with Passkey ]          │
│                                     │
│  Address  0x…   Balance  … ETH      │  ← after success only
│  [ Refresh ]  [ Copy ]  [ Send ]    │
│                                     │
│  ▸ Advanced (import / export)       │  ← collapsed; not the default
└─────────────────────────────────────┘
```

---

## 8. Suggested next engineering steps (after you choose)

### If Turnkey (A)

1. Create Turnkey org; enable Auth Proxy + passkeys; set RP ID.  
2. Fork patterns from `demo-embedded-wallet` into a small static/Next page or in-app WebView.  
3. Bridge: JS posts `{address, session}` → Android/desktop wallet panel.  
4. Sign txs via Turnkey SDK; broadcast to RH RPC.

### If OSS local (B)

1. Port w3pk-style register/login into `android/…/assets/wallet/passkey.html` (WebView).  
2. Use WebAuthn + AES-GCM encrypted key material; optional PRF where available.  
3. Bridge address + sign requests via `JavascriptInterface` / Chrome messaging.  
4. Desktop: open same page in local HTTP server + system browser, or WebView widget.

### If 4337 (C)

1. Confirm EntryPoint + P-256 verifier availability on chain 4663.  
2. Deploy factory (or use Alchemy AA if compatible).  
3. WebAuthn create → counterfactual address → UserOp send.  
4. UI same one-button pattern; balance via EntryPoint/account.

---

## 9. Sources

### Live reverse engineering

- https://hood.dev HTML + `/_next/static/chunks/*.js` (2026-07-29)  
  Markers: `api.turnkey.com`, `authproxy.turnkey.com`, `@turnkey/core@2.2.0`, passkey stamper, createWallet/sub-org.

### Cloned OSS (local)

- `tools/passkey-research/turnkey-demo-passkey-wallet`  
- `tools/passkey-research/demo-embedded-wallet`  
- `tools/passkey-research/passkeys-4337-smart-wallet`  
- `tools/passkey-research/daimo-p256-verifier`  
- `tools/passkey-research/base-webauthn-sol`  
- `tools/passkey-research/w3pk`  
- `tools/passkey-research/portkey-client`

### Industry references

- Turnkey embedded auth: https://www.turnkey.com/blog/embedded-wallet-authentication  
- WebAuthn PRF for key derivation (Polkadot forum, Corbado, passkeyprf.com)  
- ERC-4337 + passkeys (Stackup / passkeys-4337)  
- Curve mismatch: P-256 vs secp256k1 (multiple wallet engineering posts; w3pk ARCHITECTURE.md)

---

## 10. Bottom line

**Research is complete.**  

- **hood.dev** = Turnkey passkey + sub-org + enclave wallet (verified in production JS).  
- **The AI reply** = useful intuition, **wrong on hood.dev stack and on “derive EOA from passkey pubkey.”**  
- **This app’s current Wallet tab** = honest EOA keystore prototype, **not** the passkey product.  
- **Next action is a product choice**, not more private-key UI:

  1. **Turnkey** (closest to hood.dev) — need org credentials from you, or  
  2. **OSS Path B WebView** (self-hosted, more engineering), or  
  3. **OSS Path C 4337** (correct pure passkey signer, heaviest chain infra).

Until that choice is made and implemented, the correct status is:  
**“Passkey wallet not shippable yet; research done; blockers documented.”**
