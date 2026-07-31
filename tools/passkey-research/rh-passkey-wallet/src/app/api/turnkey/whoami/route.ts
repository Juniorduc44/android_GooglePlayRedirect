import { NextResponse } from "next/server";
import { whoami } from "@/server/turnkey";

/** POST /api/turnkey/whoami — stamped query via parent API key */
export async function POST() {
  try {
    const result = await whoami();
    return NextResponse.json({ ok: true, result });
  } catch (e) {
    return NextResponse.json(
      {
        ok: false,
        error: e instanceof Error ? e.message : String(e),
      },
      { status: 502 },
    );
  }
}

export async function GET() {
  return POST();
}
