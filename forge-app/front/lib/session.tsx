"use client"

import { createContext, useContext } from "react"
import type { Me } from "@/lib/roles"

const SessionContext = createContext<Me | null>(null)

export function SessionProvider({ me, children }: { me: Me; children: React.ReactNode }) {
  return <SessionContext.Provider value={me}>{children}</SessionContext.Provider>
}

/** Profil de l'utilisateur connecté, fourni par AppShell. */
export function useSession(): Me {
  const me = useContext(SessionContext)
  if (!me) throw new Error("useSession doit être utilisé à l'intérieur de <AppShell>")
  return me
}
