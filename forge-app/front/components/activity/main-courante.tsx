"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { toast } from "sonner"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { api, ApiError } from "@/lib/api"
import type { Activity } from "@/lib/activity"
import { useCurrentBusiness } from "@/lib/current-business"
import { useSession } from "@/lib/session"

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

const LABELS: Record<string, string> = {
  LOGIN_OK: "Connexion", LOGIN_FAIL: "Connexion refusée",
  MEMBER_ADD: "Ajout membre", ROLE_SET: "Changement de rôle",
  CREANCE_DEPOT: "Dépôt créance", CREANCE_PAIEMENT: "Paiement créance",
}
const label = (a: string) => LABELS[a] ?? a

export function MainCourante() {
  const me = useSession()
  const isSystem = me.user.globalRole === "SYSTEM"
  const { currentId } = useCurrentBusiness()
  const [scope, setScope] = useState<"business" | "system">("business")
  const [limit, setLimit] = useState(200)
  const [rows, setRows] = useState<Activity[]>([])

  const load = useCallback(() => {
    const url = scope === "system"
      ? `/api/system/activity?limit=${limit}`
      : currentId ? `/api/businesses/${currentId}/activity?limit=${limit}` : null
    if (!url) { setRows([]); return }
    api<Activity[]>(url).then(setRows).catch(fail)
  }, [scope, currentId, limit])
  useEffect(() => load(), [load])

  const byDay = useMemo(() => {
    const map = new Map<string, Activity[]>()
    for (const e of rows) {
      const d = new Date(e.createdAt).toLocaleDateString("fr-FR", { weekday: "long", day: "2-digit", month: "long", year: "numeric" })
      const bucket = map.get(d) ?? []
      bucket.push(e)
      map.set(d, bucket)
    }
    return Array.from(map)
  }, [rows])

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Main courante</h1>
          <p className="mt-1 text-sm text-muted-foreground">Journal d&apos;activité, regroupé par jour.</p>
        </div>
        {isSystem ? (
          <div className="flex gap-1 rounded-md border p-1">
            <Button variant={scope === "business" ? "default" : "ghost"} size="sm" onClick={() => { setScope("business"); setLimit(200) }}>Business</Button>
            <Button variant={scope === "system" ? "default" : "ghost"} size="sm" onClick={() => { setScope("system"); setLimit(200) }}>Système</Button>
          </div>
        ) : null}
      </div>

      {scope === "business" && !currentId ? (
        <p className="text-sm text-muted-foreground">Sélectionne un business (en haut).</p>
      ) : null}

      {byDay.map(([day, items]) => (
        <div key={day} className="rounded-md border">
          <div className="sticky top-0 border-b bg-card/95 px-4 py-2 text-sm font-semibold capitalize backdrop-blur">{day}</div>
          <div className="flex flex-col divide-y">
            {items.map((e, i) => (
              <div key={i} className="flex flex-wrap items-center gap-3 px-4 py-2 text-sm">
                <span className="w-12 shrink-0 tabular-nums text-muted-foreground">
                  {new Date(e.createdAt).toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}
                </span>
                <Badge variant="outline">{label(e.action)}</Badge>
                <span className="min-w-0 flex-1 truncate">{e.details ?? ""}</span>
                <span className="shrink-0 text-muted-foreground">{e.username}</span>
              </div>
            ))}
          </div>
        </div>
      ))}

      {rows.length === 0 ? <p className="text-sm text-muted-foreground">Aucune activité.</p> : null}
      {rows.length >= limit ? (
        <Button variant="outline" className="self-center" onClick={() => setLimit((l) => l + 200)}>Charger plus</Button>
      ) : null}
    </div>
  )
}
