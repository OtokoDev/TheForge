"use client"

import { useEffect } from "react"
import { Hammer, LogIn, ShieldCheck, Store } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { LOGIN_URL } from "@/lib/api"
import { useMe } from "@/lib/use-me"

export default function Home() {
  const { state } = useMe()

  // Déjà connecté → aller au tableau de bord.
  useEffect(() => {
    if (state === "ready") window.location.href = "/dashboard"
  }, [state])

  return (
    <main className="min-h-screen bg-background">
      <section className="mx-auto flex min-h-screen w-full max-w-6xl flex-col justify-center gap-10 px-6 py-10">
        <div className="max-w-3xl">
          <div className="mb-6 flex size-14 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <Hammer data-icon="inline-start" />
          </div>
          <h1 className="max-w-2xl text-5xl font-semibold tracking-normal text-foreground sm:text-6xl">
            Forge RP
          </h1>
          <p className="mt-5 max-w-2xl text-lg leading-8 text-muted-foreground">
            Gestion de stock, comptes et mouvements pour les activités économiques d'un
            serveur roleplay, connectée à Discord.
          </p>
          <div className="mt-8">
            <Button
              size="lg"
              disabled={state === "loading"}
              onClick={() => {
                window.location.href = LOGIN_URL
              }}
            >
              <LogIn data-icon="inline-start" />
              Se connecter avec Discord
            </Button>
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          {[
            {
              icon: Store,
              title: "Multi-business",
              text: "Forge, compagnie et autres activités, cloisonnées par business.",
            },
            {
              icon: Hammer,
              title: "Stock & comptes",
              text: "Journal de mouvements traçable, stock et trésorerie unifiés.",
            },
            {
              icon: ShieldCheck,
              title: "Rôles",
              text: "Accès SYSTEM, STAFF et par business (ADMIN / MEMBRE) après connexion Discord.",
            },
          ].map((item) => (
            <Card key={item.title}>
              <CardContent className="flex gap-4 p-5">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-md bg-accent text-primary">
                  <item.icon data-icon="inline-start" />
                </div>
                <div>
                  <h2 className="font-semibold">{item.title}</h2>
                  <p className="mt-1 text-sm leading-6 text-muted-foreground">{item.text}</p>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </section>
    </main>
  )
}
