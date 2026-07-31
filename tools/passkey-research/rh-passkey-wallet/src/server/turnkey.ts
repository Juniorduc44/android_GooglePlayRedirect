/**
 * Server-side Turnkey client (Path B).
 *
 * All Turnkey calls are HTTP POST with an API-key **stamp** (X-Stamp header).
 * See: https://docs.turnkey.com/api-reference/overview/intro
 *      https://docs.turnkey.com/api-reference/overview/stamps
 *
 * Requires env (never expose to the browser):
 *   API_PUBLIC_KEY   — compressed P-256 pubkey (starts with 02 or 03)
 *   API_PRIVATE_KEY  — hex private key from Turnkey dashboard
 *   NEXT_PUBLIC_ORGANIZATION_ID — parent org
 *   BASE_URL or NEXT_PUBLIC_BASE_URL — default https://api.turnkey.com
 */

import {
  Turnkey,
  DEFAULT_ETHEREUM_ACCOUNTS,
} from "@turnkey/sdk-server";
import type { v1Attestation } from "@turnkey/sdk-types";

export type TurnkeyEnvStatus = {
  hasOrgId: boolean;
  hasApiPublic: boolean;
  hasApiPrivate: boolean;
  hasAuthProxyId: boolean;
  baseUrl: string;
  orgIdPrefix: string;
  /** True when server can stamp queries/submissions */
  serverReady: boolean;
  /** True when frontend Auth Proxy path can run */
  authProxyReady: boolean;
};

export function getTurnkeyEnv(): {
  orgId: string;
  baseUrl: string;
  apiPublicKey: string;
  apiPrivateKey: string;
  authProxyId: string;
} {
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

export function turnkeyEnvStatus(): TurnkeyEnvStatus {
  const e = getTurnkeyEnv();
  return {
    hasOrgId: Boolean(e.orgId),
    hasApiPublic: Boolean(e.apiPublicKey),
    hasApiPrivate: Boolean(e.apiPrivateKey),
    hasAuthProxyId: Boolean(e.authProxyId),
    baseUrl: e.baseUrl,
    orgIdPrefix: e.orgId ? e.orgId.slice(0, 8) : "",
    serverReady: Boolean(e.orgId && e.apiPublicKey && e.apiPrivateKey),
    authProxyReady: Boolean(e.orgId && e.authProxyId),
  };
}

/** Throws if server API credentials missing. */
export function requireServerTurnkey(): InstanceType<typeof Turnkey> {
  const e = getTurnkeyEnv();
  if (!e.orgId) {
    throw new Error("Missing NEXT_PUBLIC_ORGANIZATION_ID");
  }
  if (!e.apiPublicKey || !e.apiPrivateKey) {
    throw new Error(
      "Missing API_PUBLIC_KEY / API_PRIVATE_KEY. Create an API key in Turnkey Dashboard → API Keys and add both to .env.local (server-only).",
    );
  }
  return new Turnkey({
    apiBaseUrl: e.baseUrl,
    apiPublicKey: e.apiPublicKey,
    apiPrivateKey: e.apiPrivateKey,
    defaultOrganizationId: e.orgId,
  });
}

export function apiClient() {
  return requireServerTurnkey().apiClient();
}

/** Query: whoami (stamped POST /public/v1/query/whoami) */
export async function whoami() {
  const client = apiClient();
  const e = getTurnkeyEnv();
  return client.getWhoami({ organizationId: e.orgId });
}

/** Query: list wallets in parent org */
export async function listWallets() {
  const client = apiClient();
  const e = getTurnkeyEnv();
  return client.getWallets({ organizationId: e.orgId });
}

/** Query: sub-org IDs (optionally filter) */
export async function getSubOrgIds(filter?: {
  filterType?: string;
  filterValue?: string;
}) {
  const client = apiClient();
  const e = getTurnkeyEnv();
  return client.getSubOrgIds({
    organizationId: e.orgId,
    ...(filter?.filterType ? { filterType: filter.filterType as any } : {}),
    ...(filter?.filterValue ? { filterValue: filter.filterValue } : {}),
  });
}

/**
 * Submission: create sub-org with passkey + temp API key + ETH wallet.
 * Mirrors official with-passkeys/with-backend createSuborgAction.
 */
export async function createPasskeySubOrg(params: {
  label: string;
  challenge: string;
  attestation: v1Attestation;
  tempPublicKey: string;
}) {
  const client = apiClient();
  const res = await client.createSubOrganization({
    subOrganizationName: `rh-pk-${params.label}-${Date.now()}`.slice(0, 200),
    rootQuorumThreshold: 1,
    rootUsers: [
      {
        userName: params.label,
        userEmail: params.label.includes("@") ? params.label : undefined,
        oauthProviders: [],
        authenticators: [
          {
            authenticatorName: "Passkey",
            challenge: params.challenge,
            attestation: params.attestation,
          },
        ],
        apiKeys: [
          {
            apiKeyName: "session-bootstrap",
            publicKey: params.tempPublicKey,
            curveType: "API_KEY_CURVE_P256",
            expirationSeconds: "60",
          },
        ],
      },
    ],
    wallet: {
      walletName: "Robinhood Chain",
      accounts: [...DEFAULT_ETHEREUM_ACCOUNTS],
    },
  });

  if (
    res.activity?.status !== "ACTIVITY_STATUS_COMPLETED" ||
    !res.subOrganizationId
  ) {
    throw new Error(
      `Sub-org not created (status: ${res.activity?.status}). ` +
        `API key must self-approve CREATE_SUB_ORGANIZATION (root quorum = 1).`,
    );
  }

  return res;
}
