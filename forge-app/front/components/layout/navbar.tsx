"use client"

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { SelectField } from "@/components/ui/select-field"
import { LOGOUT_URL } from "@/lib/api"
import { useCurrentBusiness } from "@/lib/current-business"
import { GLOBAL_ROLE_LABELS, type Me } from "@/lib/roles"
import { useShift } from "@/lib/shift"

function ShiftIndicator() {
  const { shift } = useShift()
  if (!shift?.open) return null
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/40 bg-emerald-500/15 px-2.5 py-0.5 text-xs font-medium text-emerald-400">
      <span className="size-1.5 animate-pulse rounded-full bg-emerald-400" />
      Service en cours
    </span>
  )
}

function BusinessSelector() {
  const { businesses, currentId, setCurrentId } = useCurrentBusiness()

  if (businesses.length === 0) {
    return <span className="text-xs text-muted-foreground">Aucun business</span>
  }

  return (
    <SelectField
      ariaLabel="Business courant"
      value={currentId ?? ""}
      onChange={setCurrentId}
      options={businesses.map((b) => ({ value: b.id, label: b.nom }))}
    />
  )
}

export function Navbar({ me }: { me: Me }) {
  const user = me.user
  const displayName = user.inGameName ?? user.username
  const initials = displayName.slice(0, 2).toUpperCase()

  return (
    <header className="flex min-h-16 items-center justify-between gap-3 border-b bg-background/75 px-4 backdrop-blur lg:px-8">
      <div className="flex items-center gap-3">
        <div className="hidden sm:block">
          <p className="text-sm text-muted-foreground">Forge RP</p>
          <h1 className="text-xl font-semibold">Tableau de bord</h1>
        </div>
        <BusinessSelector />
        <ShiftIndicator />
      </div>

      <div className="flex items-center gap-3">
        <Badge variant="outline">{GLOBAL_ROLE_LABELS[user.globalRole]}</Badge>
        <Avatar className="size-9">
          <AvatarImage src={user.avatar ?? undefined} alt={user.username} />
          <AvatarFallback>{initials}</AvatarFallback>
        </Avatar>
        <div className="hidden text-right sm:block">
          <p className="text-sm font-medium">{displayName}</p>
          <p className="text-xs text-muted-foreground">@{user.username}</p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => {
            window.location.href = LOGOUT_URL
          }}
        >
          Déconnexion
        </Button>
      </div>
    </header>
  )
}
