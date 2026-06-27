"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { toast } from "sonner"
import { api, ApiError } from "@/lib/api"
import type { Item, Product } from "@/lib/catalog"
import { useCurrentBusiness } from "@/lib/current-business"
import { canAdminBusiness } from "@/lib/roles"
import { useSession } from "@/lib/session"

const ORANGE = "#E8590C"
const TEXT = "#F4F1EE"
const MUTED = "#8f8880"
const CARD = "#1c1a18"
const TABLE_BG = "#1a1816"
const HEAD_BG = "#221f1b"
const BORDER = "1px solid rgba(255,255,255,0.07)"
const DEFAULT_CAT = "#7d90a6"
const fmt = (n: number) => n.toLocaleString("fr-FR")

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

function dedupe(items: Item[], idKey: "familyId" | "materialId", nameKey: "familyName" | "materialName") {
  const m = new Map<string, string>()
  for (const i of items) {
    const id = i[idKey]
    const nom = i[nameKey]
    if (id && nom) m.set(id, nom)
  }
  return Array.from(m, ([id, nom]) => ({ id, nom }))
}

export function BusinessCatalogue() {
  const me = useSession()
  const { currentId } = useCurrentBusiness()
  const canEdit = currentId ? canAdminBusiness(me, currentId) : false

  const [items, setItems] = useState<Item[]>([])
  const [products, setProducts] = useState<Map<string, Product>>(new Map())
  const [costs, setCosts] = useState<Map<string, number>>(new Map())
  const [query, setQuery] = useState("")
  const [fam, setFam] = useState("all")
  const [mat, setMat] = useState("all")
  const [onlySellable, setOnlySellable] = useState(false)

  const load = useCallback(() => {
    if (!currentId) return
    api<Item[]>("/api/catalog/items").then(setItems).catch(fail)
    api<Product[]>(`/api/businesses/${currentId}/products`).then((rows) => setProducts(new Map(rows.map((p) => [p.itemId, p])))).catch(fail)
    api<{ itemId: string; cost: number }[]>(`/api/businesses/${currentId}/costs`).then((rows) => setCosts(new Map(rows.map((c) => [c.itemId, c.cost])))).catch(fail)
  }, [currentId])
  useEffect(() => load(), [load])

  const visible = useMemo(() => items.filter((i) => !i.system), [items])
  const fams = useMemo(() => dedupe(visible, "familyId", "familyName"), [visible])
  const mats = useMemo(() => dedupe(visible, "materialId", "materialName"), [visible])
  const rows = useMemo(() => {
    const q = query.trim().toLowerCase()
    return visible.filter((i) =>
      (fam === "all" || i.familyId === fam) &&
      (mat === "all" || i.materialId === mat) &&
      (!onlySellable || products.get(i.id)?.prixRevente != null) &&
      (q === "" || i.name.toLowerCase().includes(q)))
  }, [visible, fam, mat, query, onlySellable, products])

  if (!currentId) {
    return <p className="text-sm text-muted-foreground">Sélectionne un business (en haut) pour gérer son catalogue.</p>
  }

  const th: React.CSSProperties = { color: MUTED, fontWeight: 600, fontSize: 12, letterSpacing: ".03em", padding: "12px 16px", borderBottom: BORDER, whiteSpace: "nowrap", textAlign: "left" }

  return (
    <div style={{ fontFamily: "system-ui,-apple-system,'Segoe UI',sans-serif" }}>
      <div style={{ marginBottom: 14 }}>
        <div style={{ color: TEXT, fontSize: 24, fontWeight: 700 }}>Catalogue</div>
        <div style={{ color: MUTED, fontSize: 13.5, marginTop: 3 }}>
          Valeur (coût) et prix de revente par produit{canEdit ? "" : " — lecture seule (admin requis)"}.
        </div>
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 14 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 11, flexWrap: "wrap" }}>
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Rechercher un produit…"
            style={{ background: CARD, border: "1px solid rgba(255,255,255,0.1)", borderRadius: 9, color: TEXT, fontSize: 13.5, padding: "9px 12px", width: 240, outline: "none", fontFamily: "inherit" }} />
          <label style={{ display: "flex", alignItems: "center", gap: 6, color: MUTED, fontSize: 13 }}>
            <input type="checkbox" checked={onlySellable} onChange={(e) => setOnlySellable(e.target.checked)} /> Vendables uniquement
          </label>
        </div>
        <FilterRow label="Famille" value={fam} onChange={setFam} options={fams} />
        <FilterRow label="Matériau" value={mat} onChange={setMat} options={mats} />
      </div>

      <div style={{ overflow: "auto", border: BORDER, borderRadius: 12, background: TABLE_BG }}>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13.5 }}>
          <thead>
            <tr style={{ background: HEAD_BG }}>
              <th style={th}>OBJET</th>
              <th style={th}>FAMILLE</th>
              <th style={th}>MATÉRIAU</th>
              <th style={{ ...th, textAlign: "right" }}>VALEUR (COÛT)</th>
              <th style={{ ...th, textAlign: "right" }}>PRIX DE REVENTE</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((it) => (
              <CatalogueRow key={it.id} businessId={currentId} item={it} product={products.get(it.id)} cost={costs.get(it.id)} canEdit={canEdit} onSaved={load} />
            ))}
          </tbody>
        </table>
        <div style={{ padding: "11px 16px", color: "#6f6862", fontSize: 12.5, borderTop: "1px solid rgba(255,255,255,0.05)" }}>{rows.length} produits</div>
      </div>
    </div>
  )
}

function CatalogueRow({ businessId, item, product, cost, canEdit, onSaved }: {
  businessId: string
  item: Item
  product: Product | undefined
  cost: number | undefined
  canEdit: boolean
  onSaved: () => void
}) {
  const [valeur, setValeur] = useState("")
  const [prix, setPrix] = useState("")
  useEffect(() => {
    setValeur(product?.valeur != null ? String(product.valeur) : "")
    setPrix(product?.prixRevente != null ? String(product.prixRevente) : "")
  }, [product])

  async function save() {
    if (!canEdit) return
    try {
      await api<Product>(`/api/businesses/${businessId}/products/${item.id}`, {
        method: "PUT",
        body: JSON.stringify({
          valeur: item.hasRecipe || valeur === "" ? null : Number(valeur),
          prixRevente: prix === "" ? null : Number(prix),
          version: product?.version ?? 0,
        }),
      })
      onSaved()
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) { toast.warning(err.message); onSaved() } else fail(err)
    }
  }

  const td: React.CSSProperties = { padding: "9px 16px", borderBottom: BORDER, color: "#cfc8c2" }
  const inputStyle: React.CSSProperties = { width: 90, textAlign: "right", background: "#15110e", border: "1px solid rgba(255,255,255,0.12)", borderRadius: 6, color: TEXT, fontSize: 13, padding: "5px 8px", outline: "none", fontFamily: "inherit" }

  return (
    <tr>
      <td style={{ ...td }}>
        <span style={{ display: "inline-flex", alignItems: "center", gap: 9 }}>
          <span style={{ width: 10, height: 10, borderRadius: 3, background: item.familyColor ?? DEFAULT_CAT, flex: "none" }} />
          <span style={{ color: TEXT, fontWeight: 600 }}>{item.name}</span>
        </span>
      </td>
      <td style={td}>{item.familyName ?? "—"}</td>
      <td style={td}>{item.materialName ?? "—"}</td>
      <td style={{ ...td, textAlign: "right" }}>
        {item.hasRecipe ? (
          <span style={{ color: MUTED }}>{cost != null ? `${fmt(cost)} (calc.)` : "—"}</span>
        ) : canEdit ? (
          <input type="number" min={0} step="0.1" value={valeur} onChange={(e) => setValeur(e.target.value)} onBlur={save} style={inputStyle} />
        ) : (
          <span>{product?.valeur != null ? fmt(product.valeur) : "—"}</span>
        )}
      </td>
      <td style={{ ...td, textAlign: "right" }}>
        {canEdit ? (
          <input type="number" min={0} step="0.1" value={prix} onChange={(e) => setPrix(e.target.value)} onBlur={save} style={inputStyle} />
        ) : (
          <span style={{ color: product?.prixRevente != null ? TEXT : MUTED }}>{product?.prixRevente != null ? fmt(product.prixRevente) : "non vendable"}</span>
        )}
      </td>
    </tr>
  )
}

function FilterRow({ label, value, onChange, options }: {
  label: string
  value: string
  onChange: (v: string) => void
  options: { id: string; nom: string }[]
}) {
  if (options.length === 0) return null
  const chip = (active: boolean): React.CSSProperties => ({
    background: active ? ORANGE : "#1f1d1b", color: active ? "#fff" : "#cfc8c2",
    border: active ? `1px solid ${ORANGE}` : "1px solid rgba(255,255,255,0.08)",
    borderRadius: 8, padding: "5px 11px", fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit",
  })
  return (
    <div style={{ display: "flex", gap: 6, flexWrap: "wrap", alignItems: "center" }}>
      <span style={{ color: MUTED, fontSize: 11, textTransform: "uppercase", letterSpacing: ".05em", width: 64 }}>{label}</span>
      <button onClick={() => onChange("all")} style={chip(value === "all")}>Tous</button>
      {options.map((o) => (
        <button key={o.id} onClick={() => onChange(o.id)} style={chip(value === o.id)}>{o.nom}</button>
      ))}
    </div>
  )
}
