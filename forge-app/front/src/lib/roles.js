// Modèle de rôles côté front, aligné sur le back (CDC §5.2). Fonctions pures.

/** Peut accéder à l'administration : SYSTEM, ou ADMIN d'au moins un business. */
export function canAdminister(me) {
  return me.user.globalRole === 'SYSTEM' || me.memberships.some((m) => m.role === 'ADMIN')
}

/** Supervision globale (lecture seule, tous business) : STAFF ou SYSTEM. */
export function canStaffView(me) {
  return me.user.globalRole === 'STAFF' || me.user.globalRole === 'SYSTEM'
}

/** Peut configurer un business donné : SYSTEM, ou ADMIN de ce business. */
export function canAdminBusiness(me, businessId) {
  return (
    me.user.globalRole === 'SYSTEM' ||
    me.memberships.some((m) => m.businessId === businessId && m.role === 'ADMIN')
  )
}

/** Peut opérer dans un business (mouvements…) : SYSTEM ou membre (ADMIN/MEMBRE). */
export function canOperateBusiness(me, businessId) {
  return (
    me.user.globalRole === 'SYSTEM' ||
    me.memberships.some((m) => m.businessId === businessId)
  )
}

export const GLOBAL_ROLE_LABELS = {
  SYSTEM: 'Système',
  STAFF: 'Staff',
  NONE: 'Membre',
}
