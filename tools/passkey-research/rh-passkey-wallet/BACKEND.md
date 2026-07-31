# Backend + CLI (stamped Turnkey API)

## What the API docs mean for us

From [API intro](https://docs.turnkey.com/api-reference/overview/intro):

| Rule | Meaning |
|---|---|
| **RPC over HTTP** | We call named endpoints with JSON bodies |
| **POST-only** | No GET against Turnkey core API |
| **Stamp** | Every request body is signed; enclave verifies integrity |
| **Queries** | Reads (`whoami`, `getWallets`, `getSubOrgIds`) |
| **Submissions / activities** | Writes (`createSubOrganization`, `signTransaction`, …) |

Stamp types ([stamps](https://docs.turnkey.com/api-reference/overview/stamps)):

- **`X-Stamp`** — API key (P-256) — **server / CLI**
- **`X-Stamp-Webauthn`** — passkey — **browser after user gesture**

## Two paths

| Path | Credentials | Use |
|---|---|---|
| **A Auth Proxy** | Org ID + Auth Proxy Config ID | Frontend passkey signup/login via managed proxy |
| **B Server API key** | Org ID + **API_PUBLIC_KEY** + **API_PRIVATE_KEY** | CLI, health, create sub-org from backend |

You already have Path A. **Path B is what was missing** for “fully programmable from the CLI.”

## Create parent API key (you)

1. [app.turnkey.com](https://app.turnkey.com) → your org  
2. **API Keys** → Create  
3. Save **public** (starts with `02` or `03`) and **private** hex  
4. Key must be able to call `whoami` and (for sub-orgs) `CREATE_SUB_ORGANIZATION` with **root quorum = 1** for self-approve  
5. Add to `.env.local` (never `NEXT_PUBLIC_*`):

```bash
API_PUBLIC_KEY=02...
API_PRIVATE_KEY=...
```

6. Restart `npm run dev`

## CLI commands

```bash
cd tools/passkey-research/rh-passkey-wallet

npm run tk:health    # env + stamped whoami
npm run tk:whoami
npm run tk:wallets
npm run tk:suborgs
npm run tk:stamp     # demonstrate stamp over body

# or
node --env-file=.env.local scripts/tk.mjs health
```

## HTTP (while next is running)

```bash
curl -s localhost:3456/api/turnkey/health | jq
curl -s localhost:3456/api/turnkey/whoami | jq
```

## Code map

| File | Role |
|---|---|
| `src/server/turnkey.ts` | Server SDK client + whoami / wallets / createSubOrg |
| `src/app/api/turnkey/health/route.ts` | Health + stamped probes |
| `src/app/api/turnkey/whoami/route.ts` | Whoami |
| `src/app/api/turnkey/create-suborg/route.ts` | Submission: create sub-org with passkey |
| `scripts/tk.mjs` | CLI entry |

## Security

- API private key: server + CLI only  
- Never log full private key  
- Passkey stamps still only happen in the browser after a user gesture  
