// Client API du back Micronaut. Front embarqué (même origine) → base "". En dev, le proxy
// Vite redirige /api, /oauth, /logout, /ws vers :8080.
const API_BASE = ''

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/** Appel JSON authentifié (cookie JWT via credentials: "include"). */
export async function api(path, init) {
  const res = await fetch(`${API_BASE}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(init && init.headers) },
    ...init,
  })

  if (res.status === 204) return undefined

  const body = await res.json().catch(() => null)
  if (!res.ok) {
    throw new ApiError(res.status, (body && body.message) || res.statusText)
  }
  return body
}

/** Déclenche le flux OAuth2 Discord côté Micronaut. */
export const LOGIN_URL = `${API_BASE}/oauth/login/discord`
/** Déconnexion (GET autorisé) : purge le cookie puis redirige. */
export const LOGOUT_URL = `${API_BASE}/logout`
