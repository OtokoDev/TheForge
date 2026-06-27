import Link from "next/link"
import { ShieldAlert } from "lucide-react"
import { buttonVariants } from "@/components/ui/button"

export default function AccessDeniedPage() {
  return (
    <main className="flex min-h-screen items-center justify-center px-6">
      <div className="max-w-md text-center">
        <div className="mx-auto flex size-12 items-center justify-center rounded-md bg-destructive/20 text-red-200">
          <ShieldAlert data-icon="inline-start" />
        </div>
        <h1 className="mt-5 text-3xl font-semibold">Accès refusé</h1>
        <p className="mt-3 text-sm leading-6 text-muted-foreground">
          Ton rôle Discord n’a pas encore les permissions nécessaires pour cette zone de la
          forge.
        </p>
        <Link className={buttonVariants({ className: "mt-6" })} href="/catalogue">
          Retour au catalogue
        </Link>
      </div>
    </main>
  )
}
