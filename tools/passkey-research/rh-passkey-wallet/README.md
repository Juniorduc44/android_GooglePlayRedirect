# RH Passkey Wallet (PoC)

Minimal **Turnkey passkey** login + ETH wallet surface for **Robinhood Chain (4663)**.

Built from patterns in [tkhq/sdk](https://github.com/tkhq/sdk) (`@turnkey/react-wallet-kit`).

## Docs

- Implementation plan: [`../TURNKEY_SDK_PASSKEY_PLAN.md`](../TURNKEY_SDK_PASSKEY_PLAN.md)
- SDK clone: [`../tkhq-sdk`](../tkhq-sdk)

## Setup

1. Create a Turnkey org at https://app.turnkey.com  
2. Enable **Passkeys** and create an **Auth Proxy** config  
3. Configure env:

```bash
cp .env.local.example .env.local
# fill NEXT_PUBLIC_ORGANIZATION_ID, NEXT_PUBLIC_AUTH_PROXY_ID
# NEXT_PUBLIC_RP_ID=localhost
```

4. Run:

```bash
pnpm install   # or npm install
pnpm dev       # http://localhost:3456
```

## Security notes (PoC)

- Keys stay in Turnkey enclaves; passkeys only stamp requests.
- Do not put API private keys in this frontend.
- Production needs HTTPS + real RP ID + CSP + session policies (see plan).
