import { auth } from "@/auth"
import { redirect } from "next/navigation"

export type Role = "ADMIN" | "GERANT" | "FORGERON"

const hierarchy: Role[] = ["FORGERON", "GERANT", "ADMIN"]

export function hasRole(userRole: Role | undefined, minRole: Role) {
  if (!userRole) return false
  if (minRole === "ADMIN") return userRole === "ADMIN" || userRole === "GERANT"
  return hierarchy.indexOf(userRole) >= hierarchy.indexOf(minRole)
}

export async function requireRole(minRole: Role) {
  const session = await auth()
  if (!session?.user) redirect("/")
  if (!session.user.isActive) redirect("/access-denied")
  if (!hasRole(session.user.role, minRole)) redirect("/access-denied")

  return session
}

export async function requireApiRole(minRole: Role) {
  const session = await auth()
  if (!session?.user) {
    return { error: "Non autorisé", status: 401 as const, session: null }
  }
  if (!session.user.isActive || !hasRole(session.user.role, minRole)) {
    return { error: "Accès refusé", status: 403 as const, session: null }
  }

  return { error: null, status: 200 as const, session }
}
