"use client"

import { useEffect } from "react"
import { Navbar } from "@/components/layout/navbar"
import { Sidebar } from "@/components/layout/sidebar"
import { useMe } from "@/lib/use-me"
import { SessionProvider } from "@/lib/session"
import { CurrentBusinessProvider } from "@/lib/current-business"
import { CurrentShiftProvider } from "@/lib/shift"
import { RealtimeProvider } from "@/lib/realtime"

/**
 * Coquille applicative + garde d'authentification. Charge /api/me ; si anonyme,
 * renvoie vers la page de connexion. Les pages se contentent de
 * <AppShell>…</AppShell> (plus besoin de passer l'utilisateur).
 */
export function AppShell({ children }: { children: React.ReactNode }) {
  const { me, state } = useMe()

  useEffect(() => {
    if (state === "anon") window.location.href = "/"
  }, [state])

  if (state === "loading" || state === "anon") {
    return (
      <div className="flex min-h-screen items-center justify-center text-muted-foreground">
        Chargement…
      </div>
    )
  }
  if (state === "error" || !me) {
    return (
      <div className="flex min-h-screen items-center justify-center text-destructive">
        Erreur de chargement de la session.
      </div>
    )
  }

  return (
    <SessionProvider me={me}>
      <CurrentBusinessProvider>
        <RealtimeProvider>
        <CurrentShiftProvider>
          <div className="min-h-screen bg-background">
            <div className="flex">
              <Sidebar />
              <div className="min-w-0 flex-1">
                <Navbar me={me} />
                <main className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-6 lg:px-8">
                  {children}
                </main>
              </div>
            </div>
          </div>
        </CurrentShiftProvider>
        </RealtimeProvider>
      </CurrentBusinessProvider>
    </SessionProvider>
  )
}
