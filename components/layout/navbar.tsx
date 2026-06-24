import { signOut } from "@/auth"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { RoleBadge } from "@/components/layout/role-badge"

export function Navbar({
  user,
}: {
  user: {
    name?: string | null
    inGameName?: string | null
    discordUsername?: string | null
    image?: string | null
    role: "ADMIN" | "GERANT" | "FORGERON"
  }
}) {
  const displayName = user.inGameName ?? user.name ?? "Profil"
  const initials = displayName.slice(0, 2).toUpperCase()

  return (
    <header className="flex min-h-16 items-center justify-between border-b bg-background/75 px-4 backdrop-blur lg:px-8">
      <div>
        <p className="text-sm text-muted-foreground">Forge RP</p>
        <h1 className="text-xl font-semibold">Atelier & commandes</h1>
      </div>

      <div className="flex items-center gap-3">
        <RoleBadge role={user.role} />
        <Avatar className="size-9">
          <AvatarImage src={user.image ?? undefined} alt={user.name ?? "Avatar Discord"} />
          <AvatarFallback>{initials}</AvatarFallback>
        </Avatar>
        <div className="hidden text-right sm:block">
          <p className="text-sm font-medium">{displayName}</p>
          <p className="text-xs text-muted-foreground">Session active</p>
        </div>
        <form
          action={async () => {
            "use server"
            await signOut({ redirectTo: "/" })
          }}
        >
          <Button type="submit" variant="outline" size="sm">
            Déconnexion
          </Button>
        </form>
      </div>
    </header>
  )
}
