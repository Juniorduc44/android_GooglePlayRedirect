"use client";

import { useEffect } from "react";
import {
  TurnkeyProvider,
  type TurnkeyProviderConfig,
} from "@turnkey/react-wallet-kit";
import "@turnkey/react-wallet-kit/styles.css";
import { debugLog } from "@/lib/debug";

/**
 * Config aligned with official demo:
 * tools/passkey-research/tkhq-sdk/examples/demos/with-react-wallet-kit/src/constants.ts
 *
 * Required env (Path A — Auth Proxy):
 *   NEXT_PUBLIC_ORGANIZATION_ID
 *   NEXT_PUBLIC_AUTH_PROXY_ID  (or NEXT_PUBLIC_AUTH_PROXY_CONFIG_ID)
 *   NEXT_PUBLIC_RP_ID=localhost  (dev)
 */

const orgId = process.env.NEXT_PUBLIC_ORGANIZATION_ID || "";
const authProxyId =
  process.env.NEXT_PUBLIC_AUTH_PROXY_ID ||
  process.env.NEXT_PUBLIC_AUTH_PROXY_CONFIG_ID ||
  "";
const rpId = process.env.NEXT_PUBLIC_RP_ID || "localhost";

/** Same shape as official `createSuborgParams` — ETH account on first passkey signup. */
const ethWalletOnSignup = {
  customWallet: {
    walletName: "Robinhood Chain",
    walletAccounts: [
      {
        addressFormat: "ADDRESS_FORMAT_ETHEREUM" as const,
        curve: "CURVE_SECP256K1" as const,
        pathFormat: "PATH_FORMAT_BIP32" as const,
        path: "m/44'/60'/0'/0/0",
      },
    ],
  },
};

const config: TurnkeyProviderConfig = {
  // Official field order / names from with-react-wallet-kit constants.ts
  apiBaseUrl: process.env.NEXT_PUBLIC_BASE_URL || "https://api.turnkey.com",
  authProxyUrl:
    process.env.NEXT_PUBLIC_AUTH_PROXY_URL || "https://authproxy.turnkey.com",
  authProxyConfigId: authProxyId,
  organizationId: orgId,
  importIframeUrl:
    process.env.NEXT_PUBLIC_IMPORT_IFRAME_URL || "https://import.turnkey.com",
  exportIframeUrl:
    process.env.NEXT_PUBLIC_EXPORT_IFRAME_URL || "https://export.turnkey.com",
  // Explicit RPID — on web the kit often auto-detects hostname; set for clarity
  passkeyConfig: {
    rpId,
    timeout: 60_000,
    userVerification: "preferred",
    allowCredentials: [], // discoverable credentials; filled after login by kit
  },
  auth: {
    autoRefreshSession: true,
    createSuborgParams: {
      passkeyAuth: ethWalletOnSignup,
    },
  },
  // If Auth Proxy config fetch is slow/blocked, still become Ready
  autoFetchWalletKitConfig: true,
  ui: {
    // Only passkey — single path (login + signup both use passkey inside kit modal)
    authModal: {
      methods: {
        passkeyAuthEnabled: true,
        emailOtpAuthEnabled: false,
        smsOtpAuthEnabled: false,
        walletAuthEnabled: false,
        googleOauthEnabled: false,
        appleOauthEnabled: false,
        facebookOauthEnabled: false,
        xOauthEnabled: false,
        discordOauthEnabled: false,
      },
      methodOrder: ["passkey"],
    },
    darkMode: true,
    borderRadius: 16,
    backgroundBlur: 8,
    // Modal must render inside provider tree (official demo sets this when needed)
    renderModalInProvider: true,
    preferLargeActionButtons: true,
    colors: {
      dark: {
        primary: "#2563eb",
        modalBackground: "#0b0b0b",
      },
    },
  },
};

export function Providers({ children }: { children: React.ReactNode }) {
  const missing = !orgId || !authProxyId;

  useEffect(() => {
    void debugLog("provider.mount", {
      hasOrg: Boolean(orgId),
      orgPrefix: orgId.slice(0, 8),
      hasAuthProxy: Boolean(authProxyId),
      authProxyPrefix: authProxyId.slice(0, 8),
      rpId,
      missing,
      origin:
        typeof window !== "undefined" ? window.location.origin : "(ssr)",
    });
  }, [missing]);

  if (missing) {
    return (
      <div
        style={{
          maxWidth: 560,
          margin: "48px auto",
          padding: 24,
          fontFamily: "system-ui, sans-serif",
          background: "#0f172a",
          color: "#e2e8f0",
          borderRadius: 16,
          lineHeight: 1.5,
        }}
      >
        <h1 style={{ fontSize: 20, marginTop: 0 }}>Turnkey env not configured</h1>
        <p style={{ color: "#94a3b8" }}>
          Put IDs in <code>.env.local</code> then restart <code>npm run dev</code>.
        </p>
        {children}
      </div>
    );
  }

  return (
    <TurnkeyProvider
      config={config}
      callbacks={{
        onError: (err) => {
          console.error("[turnkey.onError]", err);
          void debugLog(
            "turnkey.onError",
            {
              message: err?.message ?? String(err),
              name: (err as any)?.name,
              code: (err as any)?.code,
            },
            "error",
          );
        },
        onAuthenticationSuccess: (params) => {
          void debugLog("turnkey.onAuthenticationSuccess", {
            action: params?.action,
            method: params?.method,
            identifier: params?.identifier,
            hasSession: Boolean(params?.session),
          });
        },
      }}
    >
      {children}
    </TurnkeyProvider>
  );
}
