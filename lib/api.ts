import { NextResponse } from "next/server"
import type { Role } from "@/lib/permissions"
import { requireApiRole } from "@/lib/permissions"

export function jsonError(message: string, status = 400) {
  return NextResponse.json({ error: message }, { status })
}

export async function guardApi(minRole: Role) {
  const result = await requireApiRole(minRole)
  if (result.error) {
    return {
      response: jsonError(result.error, result.status),
      session: null,
    }
  }

  return { response: null, session: result.session }
}
