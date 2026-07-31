import { NextResponse } from "next/server";
import {
  getSubOrgIds,
  listWallets,
  turnkeyEnvStatus,
  whoami,
} from "@/server/turnkey";

/**
 * GET /api/turnkey/health
 * CLI: curl -s localhost:3456/api/turnkey/health | jq
 *
 * Reports env readiness and, if API keys present, runs stamped whoami.
 */
export async function GET() {
  const status = turnkeyEnvStatus();
  const out: Record<string, unknown> = {
    ok: true,
    timestamp: new Date().toISOString(),
    env: status,
    notes: {
      apiModel:
        "All Turnkey calls are HTTP POST + stamp header (X-Stamp or X-Stamp-Webauthn). See docs.turnkey.com/api-reference/overview/intro",
      queries: "read: whoami, getWallets, getSubOrgIds",
      submissions: "write: createSubOrganization, signTransaction, …",
      needForCli:
        "API_PUBLIC_KEY + API_PRIVATE_KEY in .env.local for server-stamped queries",
    },
  };

  if (!status.serverReady) {
    out.ok = false;
    out.error =
      "Server API keys missing. Add API_PUBLIC_KEY and API_PRIVATE_KEY to .env.local (Dashboard → API Keys). Auth Proxy alone is not enough for CLI/backend stamps.";
    return NextResponse.json(out, { status: 503 });
  }

  try {
    const me = await whoami();
    out.whoami = me;
    try {
      const wallets = await listWallets();
      out.walletCount =
        (wallets as any)?.wallets?.length ??
        (Array.isArray((wallets as any)?.wallets)
          ? (wallets as any).wallets.length
          : undefined);
    } catch (e) {
      out.walletsError = e instanceof Error ? e.message : String(e);
    }
    try {
      const subs = await getSubOrgIds();
      out.subOrgCount =
        (subs as any)?.organizationIds?.length ??
        (subs as any)?.organizationIds?.length;
      out.subOrgSample = ((subs as any)?.organizationIds || []).slice(0, 5);
    } catch (e) {
      out.subOrgsError = e instanceof Error ? e.message : String(e);
    }
    return NextResponse.json(out);
  } catch (e) {
    out.ok = false;
    out.stampError = e instanceof Error ? e.message : String(e);
    out.hint =
      "Stamp failed — check API key is for this org, not revoked, and can call whoami.";
    return NextResponse.json(out, { status: 502 });
  }
}
