import { NextRequest, NextResponse } from "next/server";
import { createPasskeySubOrg } from "@/server/turnkey";

/**
 * POST /api/turnkey/create-suborg
 * Body: { label, challenge, attestation, tempPublicKey }
 *
 * Submission activity: CREATE_SUB_ORGANIZATION (stamped by parent API key).
 * Frontend passkey ceremony produces challenge + attestation first.
 */
export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const { label, challenge, attestation, tempPublicKey } = body;
    if (!label || !challenge || !attestation || !tempPublicKey) {
      return NextResponse.json(
        {
          ok: false,
          error:
            "Required: label, challenge, attestation, tempPublicKey (from client WebAuthn + createApiKeyPair)",
        },
        { status: 400 },
      );
    }
    const result = await createPasskeySubOrg({
      label: String(label),
      challenge: String(challenge),
      attestation,
      tempPublicKey: String(tempPublicKey),
    });
    return NextResponse.json({
      ok: true,
      subOrganizationId: result.subOrganizationId,
      activityId: result.activity?.id,
      activityStatus: result.activity?.status,
    });
  } catch (e) {
    console.error("[create-suborg]", e);
    return NextResponse.json(
      { ok: false, error: e instanceof Error ? e.message : String(e) },
      { status: 502 },
    );
  }
}
