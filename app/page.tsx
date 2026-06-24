import { redirect } from "next/navigation"
import { Hammer, LogIn, ShieldCheck, Store } from "lucide-react"
import { auth, signIn } from "@/auth"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"

export default async function Home() {
  const session = await auth()
  if (session?.user) redirect("/dashboard")

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
            Gestion complète de stock, commandes, boutique et comptabilité pour une forge de
            serveur roleplay, connectée à Discord.
          </p>
          <form
            className="mt-8"
            action={async () => {
              "use server"
              await signIn("discord", { redirectTo: "/dashboard" })
            }}
          >
            <Button type="submit" size="lg">
              <LogIn data-icon="inline-start" />
              Se connecter avec Discord
            </Button>
          </form>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          {[
            {
              icon: Store,
              title: "Boutique",
              text: "Ouverture et fermeture en un clic avec notifications Discord.",
            },
            {
              icon: Hammer,
              title: "Atelier",
              text: "Recettes, matières premières, produits finis et alertes de stock.",
            },
            {
              icon: ShieldCheck,
              title: "Rôles",
              text: "Accès ADMIN, GÉRANT et FORGERON gérés après connexion Discord.",
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
