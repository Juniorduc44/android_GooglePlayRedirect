import { NextRequest, NextResponse } from "next/server";

/**
 * Browser → server log sink so UI interactions appear in the `next dev` terminal.
 * POST { level?, event, detail?, ts? }
 */
export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const level = String(body.level || "info").toUpperCase();
    const event = String(body.event || "unknown");
    const ts = body.ts || new Date().toISOString();
    const detail = body.detail;
    const line = `[UI ${level}] ${ts} · ${event}`;
    if (level === "ERROR") {
      console.error(line, detail ?? "");
    } else if (level === "WARN") {
      console.warn(line, detail ?? "");
    } else {
      console.log(line, detail ?? "");
    }
    return NextResponse.json({ ok: true });
  } catch (e) {
    console.error("[UI LOG] failed to parse body", e);
    return NextResponse.json({ ok: false }, { status: 400 });
  }
}
