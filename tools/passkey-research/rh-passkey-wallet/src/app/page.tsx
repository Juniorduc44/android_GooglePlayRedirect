"use client";

/**
 * Passkey UX aligned with official Turnkey demo:
 *   tkhq-sdk/examples/demos/with-react-wallet-kit/src/app/page.tsx
 *
 * Official pattern:
 *   - Wait for ClientState.Ready
 *   - Call handleLogin() → kit modal → user picks passkey login OR signup
 *
 * Our product preference: ONE primary action ("Continue with Passkey").
 * We open the kit modal with only passkey enabled (providers.tsx), so the
 * modal is effectively a single-method passkey flow (login + signup buttons
 * are both passkey — same credential model).
 *
 * We do NOT call loginWithPasskey() on mount (that can hang on WebAuthn get
 * with no prompt in some browsers).
 */

import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type CSSProperties,
} from "react";
import {
  AuthState,
  ClientState,
  useTurnkey,
} from "@turnkey/react-wallet-kit";
import { createPublicClient, formatEther, http } from "viem";
import { RH_CHAIN_ID, RH_EXPLORER, robinhoodChain } from "@/lib/robinhood";
import {
  debugLog,
  getLogSnapshot,
  subscribeLogs,
  type LogEntry,
} from "@/lib/debug";

function pickEthAddress(wallets: any[] | undefined): string | null {
  try {
    for (const w of wallets || []) {
      const accounts = w.accounts || w.walletAccounts || [];
      for (const a of accounts) {
        const addr = a.address || a.ethereumAddress;
        if (addr && String(addr).startsWith("0x")) return String(addr);
      }
      if (w.address && String(w.address).startsWith("0x")) {
        return String(w.address);
      }
    }
  } catch {
    /* ignore */
  }
  return null;
}

export default function HomePage() {
  const {
    authState,
    clientState,
    user,
    wallets,
    handleLogin,
    loginWithPasskey,
    signUpWithPasskey,
    logout,
    createWallet,
    refreshWallets,
    refreshUser,
  } = useTurnkey();

  const [busy, setBusy] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [balance, setBalance] = useState<string | null>(null);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [statusLine, setStatusLine] = useState("Starting…");
  const [readyTimedOut, setReadyTimedOut] = useState(false);
  const [backendHealth, setBackendHealth] = useState<string>("checking…");

  const ready = clientState === ClientState.Ready;
  const authed = authState === AuthState.Authenticated;
  const ethAddress = useMemo(() => pickEthAddress(wallets), [wallets]);

  // ---- debug panel ----
  useEffect(() => {
    setLogs(getLogSnapshot());
    return subscribeLogs((entry) => {
      setLogs((prev) => [...prev.slice(-80), entry]);
    });
  }, []);

  useEffect(() => {
    void debugLog("page.mount", {
      href: typeof window !== "undefined" ? window.location.href : null,
      userAgent:
        typeof navigator !== "undefined"
          ? navigator.userAgent.slice(0, 120)
          : null,
    });
    // Probe server stamped-API readiness (needs parent API keys)
    void (async () => {
      try {
        const r = await fetch("/api/turnkey/health");
        const j = await r.json();
        void debugLog("backend.health", j, j.ok ? "info" : "warn");
        if (j.ok) {
          setBackendHealth(
            `Server stamps OK · whoami user ${j.whoami?.userId?.slice?.(0, 8) || "?"}…`,
          );
        } else {
          setBackendHealth(
            j.env?.serverReady === false
              ? "No API_PUBLIC_KEY/API_PRIVATE_KEY — add parent org API key for CLI/backend"
              : `Server stamp fail: ${j.stampError || j.error || r.status}`,
          );
        }
      } catch (e) {
        setBackendHealth("health endpoint unreachable");
        void debugLog("backend.health.fail", e, "error");
      }
    })();
  }, []);

  // Client readiness + 20s timeout so we never sit forever on "Preparing"
  useEffect(() => {
    void debugLog("clientState.change", {
      clientState: String(clientState),
      authState: String(authState),
    });

    if (clientState === undefined || clientState === ClientState.Loading) {
      setStatusLine("Loading Turnkey client (Auth Proxy config)…");
      setReadyTimedOut(false);
      const t = window.setTimeout(() => {
        setReadyTimedOut(true);
        setStatusLine(
          "Still loading after 20s — Auth Proxy config may be blocked. Try Continue anyway, or check Allowed Origins includes this exact URL.",
        );
        void debugLog(
          "clientState.ready_timeout",
          { clientState, origin: window.location.origin },
          "warn",
        );
      }, 20_000);
      return () => window.clearTimeout(t);
    }

    if (clientState === ClientState.Error) {
      setStatusLine(
        "Turnkey client error — check Organization ID, Auth Proxy Config ID, and Allowed Origins.",
      );
      void debugLog("clientState.error", { clientState }, "error");
      return;
    }

    if (clientState === ClientState.Ready) {
      setReadyTimedOut(false);
      setStatusLine(
        authed
          ? "Passkey session active."
          : "Ready — tap Continue with Passkey (no WebAuthn until you tap).",
      );
    }
  }, [clientState, authState, authed]);

  const refreshBalance = useCallback(async () => {
    if (!ethAddress) {
      setBalance(null);
      return;
    }
    void debugLog("balance.fetch.start", { ethAddress });
    try {
      const client = createPublicClient({
        chain: robinhoodChain,
        transport: http(),
      });
      const wei = await client.getBalance({
        address: ethAddress as `0x${string}`,
      });
      const eth = formatEther(wei);
      setBalance(eth);
      void debugLog("balance.fetch.ok", { eth });
    } catch (e) {
      setBalance(null);
      void debugLog("balance.fetch.fail", e, "warn");
    }
  }, [ethAddress]);

  useEffect(() => {
    void refreshBalance();
  }, [refreshBalance]);

  const ensureEthWallet = useCallback(async () => {
    if (pickEthAddress(wallets)) {
      void debugLog("wallet.exists", { addr: pickEthAddress(wallets) });
      return pickEthAddress(wallets);
    }
    void debugLog("wallet.create.start");
    setStatusLine("Creating ETH account…");
    await createWallet({
      walletName: `RH-${Date.now()}`,
      accounts: ["ADDRESS_FORMAT_ETHEREUM"],
    } as any);
    const list = await refreshWallets();
    const addr = pickEthAddress(list);
    void debugLog("wallet.create.done", { addr });
    return addr;
  }, [wallets, createWallet, refreshWallets]);

  /**
   * Official single entry: open kit auth UI (passkey-only in our config).
   * WebAuthn prompt only after user picks Log in / Sign up inside the modal —
   * which is correct: create = credentials.create, login = credentials.get.
   */
  const openPasskeyFlow = useCallback(async () => {
    setErr(null);
    void debugLog("ui.continue_passkey.click", {
      clientState: String(clientState),
      authState: String(authState),
      ready,
      readyTimedOut,
      hasHandleLogin: typeof handleLogin === "function",
    });

    if (clientState === ClientState.Error) {
      setErr("Turnkey client failed to init. Check IDs + Allowed Origins.");
      return;
    }

    if (!ready && !readyTimedOut) {
      setErr("Still loading Turnkey — wait for Ready, then tap again.");
      void debugLog("ui.continue_blocked_loading", {}, "warn");
      return;
    }

    setBusy("Opening passkey…");
    setStatusLine(
      "Opening Turnkey passkey sheet — pick Log in (existing) or Sign up (new). Same passkey model either way.",
    );

    try {
      // Primary: official handleLogin modal (only passkey methods enabled)
      await handleLogin({
        title: "Continue with Passkey",
      });
      void debugLog("handleLogin.returned");

      // After modal closes successfully, kit updates authState via context
      // Give state a tick, then ensure wallet + balance
      await new Promise((r) => setTimeout(r, 300));
      try {
        await refreshUser?.();
      } catch (e) {
        void debugLog("refreshUser.fail", e, "warn");
      }
      try {
        await refreshWallets();
      } catch (e) {
        void debugLog("refreshWallets.fail", e, "warn");
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      // User closed modal is not always an Error — still log
      void debugLog("handleLogin.error", e, "error");
      setErr(msg);
      setStatusLine("Passkey flow ended with an error — see log.");
    } finally {
      setBusy(null);
    }
  }, [
    clientState,
    authState,
    ready,
    readyTimedOut,
    handleLogin,
    refreshUser,
    refreshWallets,
  ]);

  // After auth becomes true, ensure wallet exists
  useEffect(() => {
    if (!authed || !ready) return;
    void (async () => {
      void debugLog("auth.authenticated_effect", {
        walletCount: wallets?.length,
        ethAddress,
      });
      try {
        const addr = await ensureEthWallet();
        setStatusLine(
          addr
            ? `Unlocked · ${addr.slice(0, 10)}…${addr.slice(-6)}`
            : "Unlocked — create ETH account if missing.",
        );
        await refreshBalance();
      } catch (e) {
        void debugLog("auth.post_wallet.fail", e, "error");
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authed, ready]);

  const doLogout = async () => {
    setErr(null);
    setBusy("Signing out…");
    void debugLog("logout.click");
    try {
      await logout();
      setBalance(null);
      setStatusLine("Signed out. Tap Continue with Passkey anytime.");
      void debugLog("logout.ok");
    } catch (e) {
      setErr(e instanceof Error ? e.message : String(e));
      void debugLog("logout.fail", e, "error");
    } finally {
      setBusy(null);
    }
  };

  /**
   * Direct passkey actions (advanced) — only after Ready, only on click.
   * Exposed as secondary controls so we can test create vs get ceremonies.
   */
  const directLogin = async () => {
    setErr(null);
    setBusy("Passkey login…");
    void debugLog("direct.loginWithPasskey.start");
    setStatusLine("WebAuthn get — choose an existing passkey…");
    try {
      await loginWithPasskey();
      void debugLog("direct.loginWithPasskey.ok");
      await refreshWallets();
    } catch (e) {
      void debugLog("direct.loginWithPasskey.fail", e, "error");
      setErr(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(null);
    }
  };

  const directSignup = async () => {
    setErr(null);
    setBusy("Passkey signup…");
    void debugLog("direct.signUpWithPasskey.start");
    setStatusLine("WebAuthn create — register a new passkey…");
    try {
      await signUpWithPasskey({
        passkeyDisplayName: `rh-${window.location.hostname}-${Date.now()}`,
      });
      void debugLog("direct.signUpWithPasskey.ok");
      await refreshWallets();
    } catch (e) {
      void debugLog("direct.signUpWithPasskey.fail", e, "error");
      setErr(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(null);
    }
  };

  const canPrimary =
    (ready || readyTimedOut) &&
    clientState !== ClientState.Error &&
    !busy;

  return (
    <main style={{ maxWidth: 560, margin: "0 auto", padding: "28px 16px" }}>
      <header style={{ marginBottom: 18 }}>
        <p style={eyebrow}>PoC · official Turnkey handleLogin pattern</p>
        <h1 style={{ fontSize: 22, margin: "6px 0 4px" }}>
          Robinhood Chain · Passkey
        </h1>
        <p style={{ color: "#94a3b8", fontSize: 13, margin: 0 }}>
          Chain {RH_CHAIN_ID} · passkey unlocks enclave wallet · nothing runs
          WebAuthn until you tap a button
        </p>
      </header>

      <div style={statusBox}>
        <div style={{ fontSize: 12, color: "#64748b" }}>Status</div>
        <div style={{ fontSize: 14, marginTop: 4 }}>{statusLine}</div>
        <div style={{ fontSize: 11, color: "#64748b", marginTop: 6 }}>
          client: <code>{String(clientState ?? "undefined")}</code>
          {" · "}
          auth: <code>{String(authState)}</code>
          {busy ? ` · busy: ${busy}` : ""}
          {readyTimedOut ? " · readyTimeout" : ""}
        </div>
        <div style={{ fontSize: 11, color: "#94a3b8", marginTop: 6 }}>
          backend: {backendHealth}
        </div>
      </div>

      {!authed ? (
        <section style={card}>
          <p style={{ color: "#94a3b8", fontSize: 14, marginTop: 0 }}>
            One continue action opens Turnkey&apos;s passkey UI. Inside you&apos;ll
            see <strong style={{ color: "#e2e8f0" }}>Log in</strong> (existing
            passkey) or{" "}
            <strong style={{ color: "#e2e8f0" }}>Sign up</strong> (new passkey
            + wallet). Same model either way — the passkey is the account.
          </p>

          <button
            type="button"
            disabled={!canPrimary}
            onClick={() => void openPasskeyFlow()}
            onPointerDown={() =>
              void debugLog("ui.pointer.continue", {
                canPrimary,
                clientState: String(clientState),
              })
            }
            style={{
              ...btnPrimary,
              opacity: canPrimary ? 1 : 0.5,
              cursor: canPrimary ? "pointer" : "not-allowed",
            }}
          >
            {busy
              ? busy
              : clientState === ClientState.Loading || clientState === undefined
                ? readyTimedOut
                  ? "Continue with Passkey (forced)"
                  : "Waiting for Turnkey Ready…"
                : "Continue with Passkey"}
          </button>

          {/* Direct ceremonies for debugging — only fire on click */}
          <div
            style={{
              marginTop: 14,
              paddingTop: 14,
              borderTop: "1px solid #1e293b",
            }}
          >
            <div style={{ fontSize: 11, color: "#64748b", marginBottom: 8 }}>
              Debug · direct WebAuthn (only after Ready, only on click)
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              <button
                type="button"
                disabled={!ready || !!busy}
                onClick={() => void directSignup()}
                style={btnSecondary}
              >
                Direct: Sign up with passkey (create)
              </button>
              <button
                type="button"
                disabled={!ready || !!busy}
                onClick={() => void directLogin()}
                style={btnSecondary}
              >
                Direct: Log in with passkey (get)
              </button>
              <button
                type="button"
                style={btnGhost}
                onClick={() =>
                  void debugLog("ui.ping", {
                    origin: window.location.origin,
                    ready,
                    clientState: String(clientState),
                    authState: String(authState),
                    publicKeyCredential:
                      typeof window !== "undefined" &&
                      !!window.PublicKeyCredential,
                  })
                }
              >
                Test: send ping log
              </button>
            </div>
          </div>
        </section>
      ) : (
        <section style={card}>
          <p style={{ margin: "0 0 8px", color: "#34d399", fontSize: 13 }}>
            Unlocked with passkey
            {user?.userName ? ` · ${user.userName}` : ""}
          </p>
          <p
            style={{
              fontFamily: "ui-monospace, monospace",
              fontSize: 13,
              wordBreak: "break-all",
              margin: "0 0 8px",
              color: "#60a5fa",
            }}
          >
            {ethAddress || "No ETH address yet"}
          </p>
          <p style={{ margin: "0 0 16px", fontSize: 18, fontWeight: 700 }}>
            {balance != null ? `${Number(balance).toFixed(6)} ETH` : "— ETH"}
            <span style={{ color: "#64748b", fontSize: 12, fontWeight: 400 }}>
              {" "}
              on RH {RH_CHAIN_ID}
            </span>
          </p>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {!ethAddress && (
              <button
                type="button"
                disabled={!!busy}
                onClick={() => {
                  setBusy("Wallet…");
                  void ensureEthWallet()
                    .catch((e) => {
                      setErr(e instanceof Error ? e.message : String(e));
                      void debugLog("ensureEthWallet.fail", e, "error");
                    })
                    .finally(() => setBusy(null));
                }}
                style={btnPrimary}
              >
                Create ETH account
              </button>
            )}
            <button
              type="button"
              disabled={!!busy || !ethAddress}
              onClick={() => void refreshBalance()}
              style={btnSecondary}
            >
              Refresh balance
            </button>
            {ethAddress && (
              <a
                href={`${RH_EXPLORER}/address/${ethAddress}`}
                target="_blank"
                rel="noreferrer"
                style={{
                  ...btnGhost,
                  textAlign: "center",
                  textDecoration: "none",
                }}
              >
                View on explorer
              </a>
            )}
            <button
              type="button"
              disabled={!!busy}
              onClick={() => void doLogout()}
              style={btnGhost}
            >
              Sign out
            </button>
          </div>
        </section>
      )}

      {err && (
        <p
          style={{
            marginTop: 14,
            color: "#f87171",
            fontSize: 13,
            whiteSpace: "pre-wrap",
          }}
        >
          {err}
        </p>
      )}

      <section style={{ ...card, marginTop: 16 }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            marginBottom: 8,
          }}
        >
          <span style={{ fontSize: 13, fontWeight: 600 }}>Debug log</span>
          <span style={{ fontSize: 11, color: "#64748b" }}>
            mirrored to server terminal
          </span>
        </div>
        <div
          style={{
            maxHeight: 240,
            overflow: "auto",
            fontFamily: "ui-monospace, monospace",
            fontSize: 10,
            background: "#020617",
            borderRadius: 8,
            padding: 10,
            color: "#94a3b8",
          }}
        >
          {logs.length === 0 && <div>No events yet — tap ping or Continue.</div>}
          {logs.map((l, i) => (
            <div
              key={`${l.ts}-${i}`}
              style={{
                marginBottom: 6,
                color:
                  l.level === "error"
                    ? "#f87171"
                    : l.level === "warn"
                      ? "#fbbf24"
                      : "#94a3b8",
              }}
            >
              <span style={{ color: "#475569" }}>{l.ts.slice(11, 19)}</span>{" "}
              <strong>{l.event}</strong>
              {l.detail != null && (
                <pre
                  style={{
                    margin: "2px 0 0",
                    whiteSpace: "pre-wrap",
                    wordBreak: "break-word",
                    color: "#64748b",
                  }}
                >
                  {typeof l.detail === "string"
                    ? l.detail
                    : JSON.stringify(l.detail)}
                </pre>
              )}
            </div>
          ))}
        </div>
      </section>

      <footer style={{ marginTop: 20, color: "#475569", fontSize: 12 }}>
        Origin must be allowlisted in Turnkey Auth Proxy (e.g.{" "}
        <code>http://localhost:3456</code>). Hard-refresh after this update.
      </footer>
    </main>
  );
}

const eyebrow: CSSProperties = {
  color: "#64748b",
  fontSize: 11,
  letterSpacing: 1,
  textTransform: "uppercase",
  margin: 0,
};
const card: CSSProperties = {
  background: "#0f172a",
  borderRadius: 16,
  padding: 18,
  border: "1px solid #1e293b",
};
const statusBox: CSSProperties = {
  ...card,
  marginBottom: 12,
  padding: 14,
};
const btnPrimary: CSSProperties = {
  width: "100%",
  background: "#2563eb",
  color: "#fff",
  border: "none",
  borderRadius: 12,
  padding: "14px 16px",
  fontSize: 15,
  fontWeight: 600,
  cursor: "pointer",
};
const btnSecondary: CSSProperties = {
  ...btnPrimary,
  background: "#1e293b",
};
const btnGhost: CSSProperties = {
  ...btnPrimary,
  background: "transparent",
  border: "1px solid #334155",
  color: "#e2e8f0",
};
