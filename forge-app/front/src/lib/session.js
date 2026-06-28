import { writable, get } from 'svelte/store'
import { api } from './api.js'
import { subscribeBusiness } from './realtime.js'

// Session utilisateur + business courant + poste (shift), en stores partagés.
// Remplace les React Context (SessionProvider / CurrentBusinessProvider / CurrentShiftProvider).

export const me = writable(null)
export const businesses = writable([])
export const currentBusinessId = writable(null)
export const currentBusiness = writable(null)
export const currentLogo = writable(null)
export const shift = writable(null)

const STORAGE_KEY = 'forge.currentBusiness'

/** Charge la liste des business visibles + sélectionne le courant (persisté en localStorage). */
export async function initBusiness() {
  try {
    const list = await api('/api/businesses')
    businesses.set(list)
    const stored = localStorage.getItem(STORAGE_KEY)
    const valid = stored && list.some((b) => b.id === stored) ? stored : (list[0]?.id ?? null)
    setCurrentBusiness(valid)
  } catch {
    businesses.set([])
  }
}

export function setCurrentBusiness(id) {
  currentBusinessId.set(id)
  currentBusiness.set(get(businesses).find((b) => b.id === id) ?? null)
  if (id) localStorage.setItem(STORAGE_KEY, id)
  subscribeBusiness(id)
  refreshShift()
  loadLogo(id)
}

/** Charge le logo du business courant (affiché dans la navbar). */
export function loadLogo(id) {
  if (!id) {
    currentLogo.set(null)
    return
  }
  api(`/api/businesses/${id}/logo`)
    .then((l) => currentLogo.set(l.dataUrl))
    .catch(() => currentLogo.set(null))
}

/** Recharge le poste courant (prise de service) pour le business sélectionné. */
export async function refreshShift() {
  const id = get(currentBusinessId)
  if (!id) {
    shift.set(null)
    return
  }
  try {
    shift.set(await api(`/api/businesses/${id}/sessions/current`))
  } catch {
    shift.set(null)
  }
}
