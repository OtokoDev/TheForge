// Client API du back Micronaut. En prod le front est embarqué (même origine) → base "".
// En dev (front :3000, back :8080) : définir NEXT_PUBLIC_API_BASE=http://localhost:8080.
export const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? ""

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message)
    this.name = "ApiError"
  }
}

/** Appel JSON authentifié (cookie JWT envoyé via credentials: "include"). */
export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(init?.headers as Record<string, string>) },
    ...init,
  })

  if (res.status === 204) return undefined as T

  const body = await res.json().catch(() => null)
  if (!res.ok) {
    throw new ApiError(res.status, body?.message ?? res.statusText)
  }
  return body as T
}

/** URL de connexion : déclenche le flux OAuth2 Discord côté Micronaut. */
export const LOGIN_URL = `${API_BASE}/oauth/login/discord`
/** URL de déconnexion (GET autorisé) : Micronaut purge le cookie puis redirige. */
export const LOGOUT_URL = `${API_BASE}/logout`
