"use client"

import { useEffect, useRef, useState } from "react"
import { GripVertical, Trash2 } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { api, ApiError } from "@/lib/api"
import type { Taxon } from "@/lib/catalog"

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

/** Liste réordonnable (drag & drop) d'une énumération : familles ou matériaux (SYSTEM). */
export function TaxonBoard({ title, endpoint, taxa, onChanged }: {
  title: string
  endpoint: string
  taxa: Taxon[]
  onChanged: () => void
}) {
  const [rows, setRows] = useState<Taxon[]>(taxa)
  const [nom, setNom] = useState("")
  const [couleur, setCouleur] = useState("#888888")
  const drag = useRef<number | null>(null)

  useEffect(() => setRows(taxa), [taxa])

  function onDrop(target: number) {
    const from = drag.current
    drag.current = null
    if (from === null || from === target) return
    const next = [...rows]
    const [moved] = next.splice(from, 1)
    next.splice(target, 0, moved)
    setRows(next)
    api(`${endpoint}/reorder`, { method: "PUT", body: JSON.stringify({ ids: next.map((t) => t.id) }) })
      .then(() => onChanged())
      .catch(fail)
  }

  async function save(t: Taxon, patch: Partial<Pick<Taxon, "nom" | "couleur">>) {
    try {
      await api(`${endpoint}/${t.id}`, {
        method: "PUT",
        body: JSON.stringify({ nom: patch.nom ?? t.nom, ordre: t.ordre, couleur: patch.couleur ?? t.couleur, version: t.version }),
      })
      onChanged()
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) { toast.warning(err.message); onChanged() } else fail(err)
    }
  }

  async function add() {
    if (!nom.trim()) return
    try {
      await api(endpoint, { method: "POST", body: JSON.stringify({ nom: nom.trim(), couleur }) })
      setNom("")
      onChanged()
    } catch (err) {
      fail(err)
    }
  }

  async function remove(t: Taxon) {
    try {
      await api<void>(`${endpoint}/${t.id}`, { method: "DELETE" })
      onChanged()
    } catch (err) {
      fail(err)
    }
  }

  return (
    <div>
      <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        {title} — glisser-déposer pour réordonner
      </p>
      <div className="flex flex-col gap-1.5">
        {rows.map((t, i) => (
          <div
            key={t.id}
            draggable
            onDragStart={() => (drag.current = i)}
            onDragOver={(e) => e.preventDefault()}
            onDrop={() => onDrop(i)}
            className="flex items-center gap-3 rounded-md border bg-card px-3 py-2"
          >
            <GripVertical className="size-4 cursor-grab text-muted-foreground" />
            <span className="w-6 text-xs tabular-nums text-muted-foreground">{i}</span>
            <input
              type="color"
              value={t.couleur ?? "#888888"}
              onChange={(e) => save(t, { couleur: e.target.value })}
              className="size-6 shrink-0 cursor-pointer rounded border bg-transparent"
              title="Couleur"
            />
            <Input
              className="h-8 max-w-xs"
              defaultValue={t.nom}
              onBlur={(e) => e.target.value.trim() && e.target.value !== t.nom && save(t, { nom: e.target.value.trim() })}
            />
            <div className="flex-1" />
            <Button variant="ghost" size="icon" onClick={() => remove(t)}>
              <Trash2 />
            </Button>
          </div>
        ))}
        {rows.length === 0 ? <p className="text-sm text-muted-foreground">Aucune entrée.</p> : null}
      </div>
      <div className="mt-3 flex items-center gap-2 border-t pt-3">
        <input type="color" value={couleur} onChange={(e) => setCouleur(e.target.value)} className="size-6 shrink-0 cursor-pointer rounded border bg-transparent" />
        <Input className="h-8 max-w-xs" placeholder="Nouvelle entrée…" value={nom} onChange={(e) => setNom(e.target.value)} />
        <Button size="sm" onClick={add}>Ajouter</Button>
      </div>
    </div>
  )
}
