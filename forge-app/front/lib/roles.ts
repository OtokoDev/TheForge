// Modèle de rôles côté front, aligné sur le back (CDC §5.2).
export type GlobalRole = "SYSTEM" | "STAFF" | "NONE"
export type MembershipRole = "ADMIN" | "MEMBRE"
export type BusinessType = "FORGE" | "COMPAGNIE"

export type MeUser = {
  id: string
  discordId: string
  username: string
  inGameName: string | null
  avatar: string | null
  globalRole: GlobalRole
  active: boolean
  webhooksEnabled: boolean
  createdAt: string
}

export type Membership = {
  businessId: string
  businessNom: string
  businessType: BusinessType
  role: MembershipRole
}

export type Me = {
  user: MeUser
  memberships: Membership[]
}

/** Peut accéder à l'écran d'administration : SYSTEM, ou ADMIN d'au moins un business. */
export function canAdminister(me: Me): boolean {
  return me.user.globalRole === "SYSTEM" || me.memberships.some((m) => m.role === "ADMIN")
}

/** Supervision globale (lecture seule, tous business) : STAFF ou SYSTEM. */
export function canStaffView(me: Me): boolean {
  return me.user.globalRole === "STAFF" || me.user.globalRole === "SYSTEM"
}

/** Peut configurer un business donné : SYSTEM, ou ADMIN de ce business. */
export function canAdminBusiness(me: Me, businessId: string): boolean {
  return (
    me.user.globalRole === "SYSTEM" ||
    me.memberships.some((m) => m.businessId === businessId && m.role === "ADMIN")
  )
}

/** Peut opérer dans un business (saisir des mouvements…) : SYSTEM ou membre (ADMIN/MEMBRE). */
export function canOperateBusiness(me: Me, businessId: string): boolean {
  return (
    me.user.globalRole === "SYSTEM" ||
    me.memberships.some((m) => m.businessId === businessId)
  )
}

export const GLOBAL_ROLE_LABELS: Record<GlobalRole, string> = {
  SYSTEM: "Système",
  STAFF: "Staff",
  NONE: "Membre",
}
