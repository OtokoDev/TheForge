"use client"

import { createContext, useCallback, useContext, useEffect, useState } from "react"
import { api } from "@/lib/api"
import type { ShiftStatus } from "@/lib/billing"
import { useCurrentBusiness } from "@/lib/current-business"

type ShiftContextValue = {
  shift: ShiftStatus | null
  refresh: () => void
}

const ShiftContext = createContext<ShiftContextValue | null>(null)

/** Suit le poste (prise de service) de l'utilisateur dans le business courant. */
export function CurrentShiftProvider({ children }: { children: React.ReactNode }) {
  const { currentId } = useCurrentBusiness()
  const [shift, setShift] = useState<ShiftStatus | null>(null)

  const refresh = useCallback(() => {
    if (!currentId) {
      setShift(null)
      return
    }
    api<ShiftStatus>(`/api/businesses/${currentId}/sessions/current`)
      .then(setShift)
      .catch(() => setShift(null))
  }, [currentId])

  useEffect(() => refresh(), [refresh])

  return <ShiftContext.Provider value={{ shift, refresh }}>{children}</ShiftContext.Provider>
}

export function useShift(): ShiftContextValue {
  const ctx = useContext(ShiftContext)
  if (!ctx) throw new Error("useShift doit être utilisé à l'intérieur de <AppShell>")
  return ctx
}
