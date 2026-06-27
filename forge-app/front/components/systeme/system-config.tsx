"use client"

import { useCallback, useEffect, useState } from "react"
import { toast } from "sonner"
import { BaseItemsBoard } from "@/components/systeme/base-items-board"
import { TaxonBoard } from "@/components/systeme/taxon-board"
import { Button } from "@/components/ui/button"
import { SettingsLayout, type SettingsTab } from "@/components/layout/settings-layout"
import { api, ApiError } from "@/lib/api"
import type { Family, Item, Material } from "@/lib/catalog"
import { useCurrentBusiness } from "@/lib/current-business"
import { useSession } from "@/lib/session"

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

const TABS: SettingsTab[] = [
  { key: "familles", label: "Familles" },
  { key: "materiaux", label: "Matériaux" },
  { key: "objets", label: "Objets de base" },
  { key: "import", label: "Import Skyrim" },
]

export function SystemConfig() {
  const me = useSession()
  const { currentId } = useCurrentBusiness()
  const [tab, setTab] = useState("familles")
  const [families, setFamilies] = useState<Family[]>([])
  const [materials, setMaterials] = useState<Material[]>([])
  const [items, setItems] = useState<Item[]>([])

  const loadTaxa = useCallback(() => {
    api<Family[]>("/api/catalog/families").then(setFamilies).catch(fail)
    api<Material[]>("/api/catalog/materials").then(setMaterials).catch(fail)
  }, [])
  const loadItems = useCallback(() => {
    api<Item[]>("/api/catalog/items").then(setItems).catch(fail)
  }, [])
  useEffect(() => loadTaxa(), [loadTaxa])
  useEffect(() => loadItems(), [loadItems])

  if (me.user.globalRole !== "SYSTEM") {
    return <p className="text-sm text-destructive">Réservé au rôle SYSTEM.</p>
  }

  async function runSeed() {
    const withPrices = !!currentId
    if (!window.confirm(`Importer le catalogue Skyrim ?\nFamilles, matériaux, items et recettes${withPrices ? "\n+ prix pour le business courant" : ""}.\n(idempotent)`)) return
    try {
      const r = await api<{ itemsCreated: number; recipesSet: number; productsCreated: number; familiesCreated: number; materialsCreated: number; warnings: string[] }>(
        `/api/catalog/seed${withPrices ? `?businessId=${currentId}` : ""}`, { method: "POST" })
      toast.success(`Seed : ${r.itemsCreated} items, ${r.recipesSet} recettes, ${r.productsCreated} prix, ${r.familiesCreated + r.materialsCreated} familles/matériaux`)
      if (r.warnings?.length) { toast.warning(`${r.warnings.length} avertissement(s) — voir console`); console.warn("Seed warnings:", r.warnings) }
      loadTaxa(); loadItems()
    } catch (err) { fail(err) }
  }

  return (
    <SettingsLayout title="Configuration — Système" subtitle="Catalogue global : familles, matériaux et objets de base (réservé SYSTEM)." tabs={TABS} active={tab} onSelect={setTab}>
      {tab === "familles" ? <TaxonBoard title="Familles" endpoint="/api/catalog/families" taxa={families} onChanged={loadTaxa} /> : null}
      {tab === "materiaux" ? <TaxonBoard title="Matériaux" endpoint="/api/catalog/materials" taxa={materials} onChanged={loadTaxa} /> : null}
      {tab === "objets" ? <BaseItemsBoard items={items} families={families} materials={materials} onChanged={loadItems} /> : null}
      {tab === "import" ? (
        <div className="flex flex-col items-start gap-3">
          <p className="text-sm text-muted-foreground">
            Importe le catalogue Skyrim (familles, matériaux, items, recettes{currentId ? " + prix du business courant" : ""}). Idempotent : ne touche pas l&apos;existant.
          </p>
          <Button onClick={runSeed}>Importer le seed Skyrim</Button>
        </div>
      ) : null}
    </SettingsLayout>
  )
}
