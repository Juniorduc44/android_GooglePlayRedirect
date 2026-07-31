/**
 * Client debug logger — console + POST /api/debug-log for server terminal visibility.
 */

export type LogLevel = "info" | "warn" | "error";

export type LogEntry = {
  ts: string;
  level: LogLevel;
  event: string;
  detail?: unknown;
};

type Listener = (entry: LogEntry) => void;

const listeners = new Set<Listener>();
const ring: LogEntry[] = [];
const MAX = 80;

function pushLocal(entry: LogEntry) {
  ring.push(entry);
  if (ring.length > MAX) ring.shift();
  listeners.forEach((fn) => fn(entry));
}

export function subscribeLogs(fn: Listener): () => void {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

export function getLogSnapshot(): LogEntry[] {
  return [...ring];
}

function serializeDetail(detail: unknown): unknown {
  if (detail == null) return undefined;
  if (detail instanceof Error) {
    return {
      name: detail.name,
      message: detail.message,
      stack: detail.stack?.split("\n").slice(0, 8),
      // TurnkeyError often has code
      ...(detail as any).code ? { code: (detail as any).code } : {},
    };
  }
  try {
    return JSON.parse(JSON.stringify(detail));
  } catch {
    return String(detail);
  }
}

export async function debugLog(
  event: string,
  detail?: unknown,
  level: LogLevel = "info",
): Promise<void> {
  const entry: LogEntry = {
    ts: new Date().toISOString(),
    level,
    event,
    detail: serializeDetail(detail),
  };
  pushLocal(entry);

  const prefix = `[RH-PK ${level.toUpperCase()}] ${event}`;
  if (level === "error") console.error(prefix, entry.detail ?? "");
  else if (level === "warn") console.warn(prefix, entry.detail ?? "");
  else console.log(prefix, entry.detail ?? "");

  try {
    await fetch("/api/debug-log", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(entry),
      keepalive: true,
    });
  } catch {
    // ignore network — console already has it
  }
}
