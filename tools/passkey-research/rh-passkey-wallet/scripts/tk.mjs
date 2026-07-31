#!/usr/bin/env node
/**
 * CLI for Turnkey stamped API (parent org API key).
 *
 * Usage (from rh-passkey-wallet/):
 *   node --env-file=.env.local scripts/tk.mjs health
 *   node --env-file=.env.local scripts/tk.mjs whoami
 *   node --env-file=.env.local scripts/tk.mjs wallets
 *   node --env-file=.env.local scripts/tk.mjs suborgs
 *   node --env-file=.env.local scripts/tk.mjs stamp-demo
 *
 * Or with running next:
 *   curl -s localhost:3456/api/turnkey/health | jq
 *
 * Docs model:
 *   - Every call is POST
 *   - Body signed → stamp header (X-Stamp for API keys)
 *   - Queries = read, Submissions = write activities
 *   https://docs.turnkey.com/api-reference/overview/intro
 *   https://docs.turnkey.com/api-reference/overview/stamps
 */

import { readFileSync, existsSync } from "node:fs";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import {
  Turnkey,
  DEFAULT_ETHEREUM_ACCOUNTS,
  signWithApiKey,
} from "@turnkey/sdk-server";

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = resolve(__dirname, "..");

function loadEnvFile() {
  const p = resolve(root, ".env.local");
  if (!existsSync(p)) return;
  for (const line of readFileSync(p, "utf8").split("\n")) {
    const t = line.trim();
    if (!t || t.startsWith("#") || !t.includes("=")) continue;
    const i = t.indexOf("=");
    const k = t.slice(0, i).trim();
    let v = t.slice(i + 1).trim();
    if (
      (v.startsWith('"') && v.endsWith('"')) ||
      (v.startsWith("'") && v.endsWith("'"))
    ) {
      v = v.slice(1, -1);
    }
    if (!process.env[k]) process.env[k] = v;
  }
}

loadEnvFile();

function env() {
  return {
    orgId: process.env.NEXT_PUBLIC_ORGANIZATION_ID || "",
    baseUrl:
      process.env.BASE_URL ||
      process.env.NEXT_PUBLIC_BASE_URL ||
      "https://api.turnkey.com",
    apiPublicKey: process.env.API_PUBLIC_KEY || "",
    apiPrivateKey: process.env.API_PRIVATE_KEY || "",
    authProxyId:
      process.env.NEXT_PUBLIC_AUTH_PROXY_ID ||
      process.env.NEXT_PUBLIC_AUTH_PROXY_CONFIG_ID ||
      "",
  };
}

function requireKeys() {
  const e = env();
  const missing = [];
  if (!e.orgId) missing.push("NEXT_PUBLIC_ORGANIZATION_ID");
  if (!e.apiPublicKey) missing.push("API_PUBLIC_KEY");
  if (!e.apiPrivateKey) missing.push("API_PRIVATE_KEY");
  if (missing.length) {
    console.error("Missing env:", missing.join(", "));
    console.error(
      "\nCreate an API key in Turnkey Dashboard → API Keys, then add to .env.local:",
    );
    console.error("  API_PUBLIC_KEY=02...or 03...");
    console.error("  API_PRIVATE_KEY=<hex>");
    console.error(
      "\nAuth Proxy Config ID alone cannot stamp server queries — see API stamps docs.",
    );
    process.exit(2);
  }
  return e;
}

function client() {
  const e = requireKeys();
  return new Turnkey({
    apiBaseUrl: e.baseUrl,
    apiPublicKey: e.apiPublicKey,
    apiPrivateKey: e.apiPrivateKey,
    defaultOrganizationId: e.orgId,
  }).apiClient();
}

function print(label, data) {
  console.log(`\n=== ${label} ===`);
  console.log(JSON.stringify(data, null, 2));
}

async function cmdHealth() {
  const e = env();
  const status = {
    orgId: e.orgId ? e.orgId.slice(0, 8) + "…" : "(missing)",
    authProxyId: e.authProxyId ? e.authProxyId.slice(0, 8) + "…" : "(missing)",
    hasApiPublic: Boolean(e.apiPublicKey),
    hasApiPrivate: Boolean(e.apiPrivateKey),
    baseUrl: e.baseUrl,
    serverReady: Boolean(e.orgId && e.apiPublicKey && e.apiPrivateKey),
    authProxyReady: Boolean(e.orgId && e.authProxyId),
  };
  print("env", status);
  if (!status.serverReady) {
    console.error("\n✗ Server API keys not set — cannot run stamped whoami.");
    process.exit(1);
  }
  const api = client();
  const me = await api.getWhoami({ organizationId: e.orgId });
  print("whoami (stamped query)", me);
  console.log("\n✓ Stamp path OK — backend can talk to Turnkey.");
}

async function cmdWhoami() {
  const e = requireKeys();
  const me = await client().getWhoami({ organizationId: e.orgId });
  print("whoami", me);
}

async function cmdWallets() {
  const e = requireKeys();
  const res = await client().getWallets({ organizationId: e.orgId });
  print("wallets", res);
}

async function cmdSuborgs() {
  const e = requireKeys();
  const res = await client().getSubOrgIds({ organizationId: e.orgId });
  print("sub-orgs", res);
}

/** Low-level stamp demo — shows the POST + stamp model without a full activity */
async function cmdStampDemo() {
  const e = requireKeys();
  const body = JSON.stringify({
    organizationId: e.orgId,
  });
  // signWithApiKey produces the stamp material used as X-Stamp
  const stamp = await signWithApiKey({
    content: body,
    publicKey: e.apiPublicKey,
    privateKey: e.apiPrivateKey,
  });
  print("stamp-demo", {
    note: "This is the cryptographic stamp over a JSON body (not sent).",
    bodyLength: body.length,
    stampPreview:
      typeof stamp === "string"
        ? stamp.slice(0, 40) + "…"
        : JSON.stringify(stamp).slice(0, 80) + "…",
    docs: "https://docs.turnkey.com/api-reference/overview/stamps",
  });
  // Also do a real stamped query
  const me = await client().getWhoami({ organizationId: e.orgId });
  print("whoami via sdk (uses same stamp model)", {
    organizationId: me.organizationId,
    organizationName: me.organizationName,
    userId: me.userId,
  });
}

async function cmdHelp() {
  console.log(`Turnkey CLI — stamped API (Path B parent org keys)

Commands:
  health      Env check + stamped whoami
  whoami      Query: whoami
  wallets     Query: list wallets (parent org)
  suborgs     Query: list sub-organization IDs
  stamp-demo  Show stamp construction + whoami
  help        This message

Env (.env.local):
  NEXT_PUBLIC_ORGANIZATION_ID   (required)
  API_PUBLIC_KEY                (required for CLI)
  API_PRIVATE_KEY               (required for CLI)
  NEXT_PUBLIC_AUTH_PROXY_ID     (frontend Auth Proxy path)
  BASE_URL                      default https://api.turnkey.com

HTTP (when next dev running):
  curl -s localhost:3456/api/turnkey/health | jq

API model:
  All Turnkey endpoints = HTTP POST + stamp header.
  Queries = reads · Submissions/Activities = writes.
`);
}

const cmd = process.argv[2] || "help";
const map = {
  health: cmdHealth,
  whoami: cmdWhoami,
  wallets: cmdWallets,
  suborgs: cmdSuborgs,
  "stamp-demo": cmdStampDemo,
  help: cmdHelp,
};

const fn = map[cmd];
if (!fn) {
  console.error("Unknown command:", cmd);
  await cmdHelp();
  process.exit(1);
}

fn().catch((e) => {
  console.error("\n✗ Error:", e?.message || e);
  if (e?.cause) console.error("cause:", e.cause);
  process.exit(1);
});
