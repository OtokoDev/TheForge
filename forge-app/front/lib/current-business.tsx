"use client"

import { createContext, useContext, useEffect, useState } from "react"
import { api } from "@/lib/api"
import type { BusinessDto } from "@/lib/business"

type CurrentBusiness = {
  businesses: BusinessDto[]
  currentId: string | null
  current: BusinessDto | null
  setCurrentId: (id: string) => void
}

const CurrentBusinessContext = createContext<CurrentBusiness | null>(null)
const STORAGE_KEY = "forge.currentBusiness"

/** Charge les business visibles et expose le « business courant » (persisté en localStorage). */
export function CurrentBusinessProvider({ children }: { children: React.ReactNode }) {
  const [businesses, setBusinesses] = useState<BusinessDto[]>([])
  const [currentId, setCurrentIdState] = useState<string | null>(null)

  useEffect(() => {
    api<BusinessDto[]>("/api/businesses")
      .then((list) => {
        setBusinesses(list)
        const stored = typeof window !== "undefined" ? window.localStorage.getItem(STORAGE_KEY) : null
        const valid = stored && list.some((b) => b.id === stored) ? stored : list[0]?.id ?? null
        setCurrentIdState(valid)
      })
      .catch(() => setBusinesses([]))
  }, [])

  function setCurrentId(id: string) {
    setCurrentIdState(id)
    if (typeof window !== "undefined") window.localStorage.setItem(STORAGE_KEY, id)
  }

  const current = businesses.find((b) => b.id === currentId) ?? null

  return (
    <CurrentBusinessContext.Provider value={{ businesses, currentId, current, setCurrentId }}>
      {children}
    </CurrentBusinessContext.Provider>
  )
}

export function useCurrentBusiness(): CurrentBusiness {
  const ctx = useContext(CurrentBusinessContext)
  if (!ctx) throw new Error("useCurrentBusiness doit être utilisé à l'intérieur de <AppShell>")
  return ctx
}
