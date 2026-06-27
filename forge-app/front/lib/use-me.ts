"use client"

import { useEffect, useState } from "react"
import { api, ApiError } from "@/lib/api"
import type { Me } from "@/lib/roles"

export type MeState = "loading" | "ready" | "anon" | "error"

/** Charge le profil courant via /api/me. 401 → anonyme (à rediriger vers le login). */
export function useMe() {
  const [me, setMe] = useState<Me | null>(null)
  const [state, setState] = useState<MeState>("loading")

  useEffect(() => {
    let active = true
    api<Me>("/api/me")
      .then((data) => {
        if (!active) return
        setMe(data)
        setState("ready")
      })
      .catch((err) => {
        if (!active) return
        setState(err instanceof ApiError && err.status === 401 ? "anon" : "error")
      })
    return () => {
      active = false
    }
  }, [])

  return { me, state }
}
