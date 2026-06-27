"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { Trash2 } from "lucide-react"
import { toast } from "sonner"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { SelectField } from "@/components/ui/select-field"
import { api, ApiError } from "@/lib/api"
import type { Family, HandRequired, Item, Material, RecipeComponentLine } from "@/lib/catalog"

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

const HAND_OPTIONS = [
  { value: "", label: "Mains : —" },
  { value: "ONE", label: "1 main" },
  { value: "TWO", label: "2 mains" },
]
const opts = (taxa: { id: string; nom: string }[], empty: string) =>
  [{ value: "", label: empty }].concat(taxa.map((t) => ({ value: t.id, label: t.nom })))

function RecipeEditor({ output, items }: { output: Item; items: Item[] }) {
  const [lines, setLines] = useState<{ componentItemId: string; quantity: number }[] | null>(null)
  const candidates = items.filter((i) => i.id !== output.id)

  const load = useCallback(() => {
    api<RecipeComponentLine[]>(`/api/catalog/items/${output.id}/recipe`)
      .then((rows) => setLines(rows.map((r) => ({ componentItemId: r.componentItemId, quantity: r.quantity }))))
      .catch(fail)
  }, [output.id])
  useEffect(() => load(), [load])

  async function save() {
    try {
      const rows = await api<RecipeComponentLine[]>(`/api/catalog/items/${output.id}/recipe`, {
        method: "PUT",
        body: JSON.stringify({ components: lines ?? [] }),
      })
      setLines(rows.map((r) => ({ componentItemId: r.componentItemId, quantity: r.quantity })))
      toast.success("Recette enregistrée")
    } catch (err) {
      fail(err)
    }
  }

  if (lines === null) return <p className="mt-2 text-xs text-muted-foreground">Chargement…</p>

  return (
    <div className="mt-2 flex flex-col gap-2">
      {lines.length === 0 ? <p className="text-xs text-muted-foreground">Aucun composant (item brut).</p> : null}
      {lines.map((line, index) => (
        <div key={index} className="flex flex-wrap items-center gap-2">
          <SelectField
            value={line.componentItemId}
            onChange={(v) => setLines((prev) => (prev ?? []).map((l, i) => (i === index ? { ...l, componentItemId: v } : l)))}
            options={candidates.map((c) => ({ value: c.id, label: c.name }))}
          />
          <Input type="number" min={1} className="w-20" value={line.quantity}
            onChange={(e) => setLines((prev) => (prev ?? []).map((l, i) => (i === index ? { ...l, quantity: Number(e.target.value) } : l)))} />
          <Button variant="ghost" size="icon" onClick={() => setLines((prev) => (prev ?? []).filter((_, i) => i !== index))}>
            <Trash2 />
          </Button>
        </div>
      ))}
      <div className="flex gap-2">
        <Button variant="outline" size="sm" disabled={candidates.length === 0}
          onClick={() => setLines((prev) => [...(prev ?? []), { componentItemId: candidates[0].id, quantity: 1 }])}>
          Ajouter un composant
        </Button>
        <Button size="sm" onClick={save}>Enregistrer la recette</Button>
      </div>
    </div>
  )
}

function ItemRow({ item, items, families, materials, onChanged }: {
  item: Item
  items: Item[]
  families: Family[]
  materials: Material[]
  onChanged: () => void
}) {
  const [editing, setEditing] = useState(false)
  const [openRecipe, setOpenRecipe] = useState(false)
  const [name, setName] = useState(item.name)
  const [familyId, setFamilyId] = useState(item.familyId ?? "")
  const [materialId, setMaterialId] = useState(item.materialId ?? "")
  const [hand, setHand] = useState<HandRequired | "">(item.handRequired ?? "")

  async function put(body: object) {
    await api<Item>(`/api/catalog/items/${item.id}`, { method: "PUT", body: JSON.stringify(body) })
    onChanged()
  }
  function onErr(err: unknown) {
    if (err instanceof ApiError && err.status === 409) { toast.warning(err.message); onChanged() } else fail(err)
  }
  async function saveEdit() {
    try {
      await put({ name: name.trim(), familyId: familyId || null, materialId: materialId || null, handRequired: hand || null, active: item.active, version: item.version })
      setEditing(false)
      toast.success("Item mis à jour")
    } catch (err) { onErr(err) }
  }
  async function toggleActive() {
    try { await put({ name: item.name, familyId: item.familyId, materialId: item.materialId, handRequired: item.handRequired, active: !item.active, version: item.version }) } catch (err) { onErr(err) }
  }
  async function remove() {
    try { await api<void>(`/api/catalog/items/${item.id}`, { method: "DELETE" }); toast.success("Item supprimé"); onChanged() } catch (err) { fail(err) }
  }

  return (
    <div className="rounded-md border bg-card px-3 py-2">
      <div className="flex flex-wrap items-center gap-2">
        <span className="font-medium">{item.name}</span>
        {item.familyName ? <Badge variant="outline" style={item.familyColor ? { borderColor: item.familyColor, color: item.familyColor } : undefined}>{item.familyName}</Badge> : null}
        {item.materialName ? <Badge variant="outline" style={item.materialColor ? { borderColor: item.materialColor, color: item.materialColor } : undefined}>{item.materialName}</Badge> : null}
        {item.handRequired ? <Badge variant="outline">{item.handRequired === "TWO" ? "2 mains" : "1 main"}</Badge> : null}
        {item.system ? <Badge variant="secondary">système</Badge> : null}
        {!item.active ? <Badge variant="destructive">inactif</Badge> : null}
        <div className="flex-1" />
        <Button variant="ghost" size="sm" onClick={() => setOpenRecipe((v) => !v)}>{openRecipe ? "Masquer recette" : "Recette"}</Button>
        {!item.system ? (
          <>
            <Button variant="ghost" size="sm" onClick={() => setEditing((v) => !v)}>{editing ? "Annuler" : "Éditer"}</Button>
            <Button variant="ghost" size="sm" onClick={toggleActive}>{item.active ? "Désactiver" : "Activer"}</Button>
            <Button variant="ghost" size="icon" onClick={remove}><Trash2 /></Button>
          </>
        ) : null}
      </div>

      {editing ? (
        <div className="mt-2 flex flex-wrap items-center gap-2 border-t pt-2">
          <Input className="max-w-xs" value={name} onChange={(e) => setName(e.target.value)} />
          <SelectField value={familyId} onChange={setFamilyId} options={opts(families, "Famille : —")} />
          <SelectField value={materialId} onChange={setMaterialId} options={opts(materials, "Matériau : —")} />
          <SelectField value={hand} onChange={(v) => setHand(v as HandRequired | "")} options={HAND_OPTIONS} />
          <Button size="sm" onClick={saveEdit}>Enregistrer</Button>
        </div>
      ) : null}

      {openRecipe ? <div className="border-t pt-2"><RecipeEditor output={item} items={items} /></div> : null}
    </div>
  )
}

export function BaseItemsBoard({ items, families, materials, onChanged }: {
  items: Item[]
  families: Family[]
  materials: Material[]
  onChanged: () => void
}) {
  const [name, setName] = useState("")
  const [familyId, setFamilyId] = useState("")
  const [materialId, setMaterialId] = useState("")
  const [hand, setHand] = useState<HandRequired | "">("")
  const [query, setQuery] = useState("")
  const [famFilter, setFamFilter] = useState("")
  const [matFilter, setMatFilter] = useState("")

  async function createItem() {
    if (!name.trim()) return
    try {
      await api<Item>("/api/catalog/items", {
        method: "POST",
        body: JSON.stringify({ name: name.trim(), familyId: familyId || null, materialId: materialId || null, handRequired: hand || null }),
      })
      setName("")
      toast.success("Item créé")
      onChanged()
    } catch (err) { fail(err) }
  }

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return items.filter((i) =>
      (q === "" || i.name.toLowerCase().includes(q)) &&
      (famFilter === "" || i.familyId === famFilter) &&
      (matFilter === "" || i.materialId === matFilter))
  }, [items, query, famFilter, matFilter])

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-md border p-3">
        <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Nouvel objet de base</p>
        <div className="flex flex-wrap items-center gap-2">
          <Input className="max-w-xs" placeholder="Nom de l'item" value={name} onChange={(e) => setName(e.target.value)} />
          <SelectField value={familyId} onChange={setFamilyId} options={opts(families, "Famille : —")} />
          <SelectField value={materialId} onChange={setMaterialId} options={opts(materials, "Matériau : —")} />
          <SelectField value={hand} onChange={(v) => setHand(v as HandRequired | "")} options={HAND_OPTIONS} />
          <Button onClick={createItem}>Créer</Button>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Input className="max-w-xs" placeholder="Rechercher…" value={query} onChange={(e) => setQuery(e.target.value)} />
        <SelectField value={famFilter} onChange={setFamFilter} options={opts(families, "Toutes familles")} />
        <SelectField value={matFilter} onChange={setMatFilter} options={opts(materials, "Tous matériaux")} />
        <span className="text-sm text-muted-foreground">{filtered.length} objet(s)</span>
      </div>

      <div className="flex flex-col gap-1.5">
        {filtered.map((item) => (
          <ItemRow key={item.id} item={item} items={items} families={families} materials={materials} onChanged={onChanged} />
        ))}
        {filtered.length === 0 ? <p className="text-sm text-muted-foreground">Aucun objet.</p> : null}
      </div>
    </div>
  )
}
