"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { toast } from "sonner"
import { api, ApiError } from "@/lib/api"
import type { Item } from "@/lib/catalog"
import { useCurrentBusiness } from "@/lib/current-business"
import type { Account, Movement, StockRow } from "@/lib/ledger"
import { useRealtime } from "@/lib/realtime"
import { canAdminBusiness, canOperateBusiness } from "@/lib/roles"
import { useSession } from "@/lib/session"

// ── Palette (DA conservée : sombre + orange forge) ───────────────────────────
const ORANGE = "#E8590C"
const TEXT = "#F4F1EE"
const MUTED = "#8f8880"
const CARD = "#1c1a18"
const TABLE_BG = "#1a1816"
const HEAD_BG = "#221f1b"
const INPUT_BG = "#15110e"
const BORDER = "1px solid rgba(255,255,255,0.07)"

const DEFAULT_CAT = "#7d90a6"
const initials = (n: string) => n.slice(0, 2).toUpperCase()
const fmt = (n: number) => n.toLocaleString("fr-FR")

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

// Valeurs distinctes (id+nom+compte) d'une dimension parmi les lignes de stock.
function distinct(rows: EnrichedRow[], idKey: "familyId" | "materialId", nameKey: "familyName" | "materialName") {
  const m = new Map<string, { nom: string; count: number }>()
  for (const r of rows) {
    const id = r[idKey]
    const nom = r[nameKey]
    if (id && nom) {
      const e = m.get(id)
      m.set(id, { nom, count: (e?.count ?? 0) + 1 })
    }
  }
  return Array.from(m, ([id, v]) => ({ id, nom: v.nom, count: v.count }))
}

type SortKey = "name" | "chest" | "qty" | "unit" | "total"
type EnrichedRow = StockRow & {
  familyId: string | null
  familyName: string | null
  materialId: string | null
  materialName: string | null
  color: string
  unit: number
  total: number
}
type Move = "DEPOSIT" | "WITHDRAW" | "TRANSFER"

export function StockPanel() {
  const me = useSession()
  const { currentId } = useCurrentBusiness()
  const canAdmin = currentId ? canAdminBusiness(me, currentId) : false
  const canOperate = currentId ? canOperateBusiness(me, currentId) : false

  const [accounts, setAccounts] = useState<Account[]>([])
  const [items, setItems] = useState<Item[]>([])
  const [prices, setPrices] = useState<Map<string, number>>(new Map())
  const [stock, setStock] = useState<StockRow[]>([])
  const [movements, setMovements] = useState<Movement[]>([])

  const [query, setQuery] = useState("")
  const [fam, setFam] = useState("all")
  const [mat, setMat] = useState("all")
  const [sort, setSort] = useState<{ key: SortKey; dir: "asc" | "desc" }>({ key: "name", dir: "asc" })

  const [inventory, setInventory] = useState(false)
  const [counted, setCounted] = useState<Record<string, string>>({})

  // panneau latéral
  const [sel, setSel] = useState<{ itemId: string; accountId: string } | null>(null)
  const [mode, setMode] = useState<Move>("DEPOSIT")
  const [qty, setQty] = useState("")
  const [motif, setMotif] = useState("")
  const [transferTo, setTransferTo] = useState("")
  const [formItem, setFormItem] = useState("")
  const [formAccount, setFormAccount] = useState("")

  const loadStock = useCallback(() => {
    if (!currentId) return
    api<StockRow[]>(`/api/businesses/${currentId}/stock`).then(setStock).catch(fail)
    api<Movement[]>(`/api/businesses/${currentId}/movements`).then(setMovements).catch(fail)
  }, [currentId])

  useEffect(() => {
    if (!currentId) return
    api<Account[]>(`/api/businesses/${currentId}/accounts`).then(setAccounts).catch(fail)
    api<Item[]>("/api/catalog/items").then(setItems).catch(fail)
    // Valeur du stock = coût de revient (valeur des matières, récursif).
    api<{ itemId: string; cost: number }[]>(`/api/businesses/${currentId}/costs`)
      .then((cs) => setPrices(new Map(cs.map((c) => [c.itemId, c.cost]))))
      .catch(fail)
  }, [currentId])
  useEffect(() => loadStock(), [loadStock])
  // Refresh live : un mouvement de stock (vente, dépôt, inventaire…) par un tiers re-fetch la liste.
  useRealtime("STOCK", loadStock)

  const itemById = useMemo(() => new Map(items.map((i) => [i.id, i])), [items])

  const enriched: EnrichedRow[] = useMemo(
    () =>
      stock.map((r) => {
        const it = itemById.get(r.itemId)
        const unit = prices.get(r.itemId) ?? 0
        return {
          ...r,
          familyId: it?.familyId ?? null, familyName: it?.familyName ?? null,
          materialId: it?.materialId ?? null, materialName: it?.materialName ?? null,
          color: it?.familyColor ?? DEFAULT_CAT, unit, total: r.quantity * unit,
        }
      }),
    [stock, itemById, prices],
  )

  const famOpts = useMemo(() => distinct(enriched, "familyId", "familyName"), [enriched])
  const matOpts = useMemo(() => distinct(enriched, "materialId", "materialName"), [enriched])

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase()
    const filtered = enriched.filter(
      (r) =>
        (fam === "all" || r.familyId === fam) &&
        (mat === "all" || r.materialId === mat) &&
        (q === "" || r.itemName.toLowerCase().includes(q)),
    )
    const dir = sort.dir === "asc" ? 1 : -1
    return [...filtered].sort((a, b) => {
      switch (sort.key) {
        case "chest": return a.accountName.localeCompare(b.accountName) * dir
        case "qty": return (a.quantity - b.quantity) * dir
        case "unit": return (a.unit - b.unit) * dir
        case "total": return (a.total - b.total) * dir
        default: return a.itemName.localeCompare(b.itemName) * dir
      }
    })
  }, [enriched, query, fam, mat, sort])

  const kpis = useMemo(() => {
    const value = enriched.reduce((s, r) => s + r.total, 0)
    const qtySum = enriched.reduce((s, r) => s + r.quantity, 0)
    const refs = new Set(enriched.map((r) => r.itemId)).size
    return { value, qty: qtySum, refs, chests: accounts.length }
  }, [enriched, accounts])

  const ecartCount = useMemo(() => {
    let n = 0
    for (const r of rows) {
      const v = counted[`${r.accountId}|${r.itemId}`]
      if (v !== undefined && v !== "" && Number(v) - r.quantity !== 0) n++
    }
    return n
  }, [rows, counted])

  function toggleSort(key: SortKey) {
    setSort((s) => (s.key === key ? { key, dir: s.dir === "asc" ? "desc" : "asc" } : { key, dir: "asc" }))
  }

  function openRow(r: EnrichedRow) {
    if (inventory) return
    setSel({ itemId: r.itemId, accountId: r.accountId })
    setMode("DEPOSIT"); setQty(""); setMotif(""); setTransferTo("")
  }

  function openNewDeposit() {
    setSel({ itemId: "", accountId: "" })
    setFormItem(items[0]?.id ?? ""); setFormAccount(accounts[0]?.id ?? "")
    setMode("DEPOSIT"); setQty(""); setMotif("")
  }

  const selItemId = sel ? (sel.itemId || formItem) : ""
  const selAccountId = sel ? (sel.accountId || formAccount) : ""
  const selRow = sel?.itemId
    ? enriched.find((r) => r.itemId === sel.itemId && r.accountId === sel.accountId)
    : undefined
  const selItem = itemById.get(selItemId)

  async function submitMove() {
    if (!currentId || !selItemId || !selAccountId) { toast.error("Item et compte requis"); return }
    const n = Number(qty)
    if (!qty || n <= 0) { toast.error("Quantité (> 0) requise"); return }
    const fromAccountId = mode === "DEPOSIT" ? null : selAccountId
    let toAccountId: string | null = mode === "DEPOSIT" ? selAccountId : null
    let type = mode === "DEPOSIT" ? "DEPOSIT" : "WITHDRAWAL"
    if (mode === "TRANSFER") {
      if (!transferTo) { toast.error("Choisis le coffre destination"); return }
      toAccountId = transferTo; type = "TRANSFER"
    }
    try {
      await api(`/api/businesses/${currentId}/movements`, {
        method: "POST",
        body: JSON.stringify({ itemId: selItemId, quantity: n, fromAccountId, toAccountId, type, note: motif || null }),
      })
      toast.success("Mouvement enregistré")
      setQty(""); setMotif("")
      loadStock()
    } catch (err) {
      fail(err)
    }
  }

  async function validateInventory() {
    if (!currentId) return
    const payload = rows
      .map((r) => ({ key: `${r.accountId}|${r.itemId}`, r }))
      .filter(({ key }) => counted[key] !== undefined && counted[key] !== "")
      .map(({ key, r }) => ({ accountId: r.accountId, itemId: r.itemId, counted: Number(counted[key]) }))
    if (payload.length === 0) { toast.error("Aucune quantité comptée"); return }
    try {
      const res = await api<{ adjusted: number }>(`/api/businesses/${currentId}/inventory`, {
        method: "POST",
        body: JSON.stringify({ counts: payload }),
      })
      toast.success(`Inventaire validé — ${res.adjusted} ligne(s) régularisée(s)`)
      setCounted({}); setInventory(false)
      loadStock()
    } catch (err) {
      fail(err)
    }
  }

  if (!currentId) {
    return <p style={{ color: MUTED, fontSize: 14 }}>Sélectionne un business (en haut) pour gérer le stock.</p>
  }

  const arrow = (key: SortKey) => (sort.key === key ? (sort.dir === "asc" ? "▲" : "▼") : "")
  const thStyle: React.CSSProperties = {
    color: MUTED, fontWeight: 600, fontSize: 12, letterSpacing: ".03em", padding: "13px 16px",
    cursor: "pointer", borderBottom: BORDER, whiteSpace: "nowrap",
  }
  const tdNum: React.CSSProperties = { padding: "11px 16px", textAlign: "right", color: TEXT, fontVariantNumeric: "tabular-nums" }

  return (
    <div style={{ fontFamily: "system-ui,-apple-system,'Segoe UI',sans-serif" }}>
      {/* header */}
      <div style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", marginBottom: 15 }}>
        <div>
          <div style={{ color: TEXT, fontSize: 24, fontWeight: 700, letterSpacing: "-0.01em" }}>Stock</div>
          <div style={{ color: MUTED, fontSize: 13.5, marginTop: 3 }}>Inventaire commun · tous les coffres</div>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          {canAdmin ? (
            <button
              onClick={() => { setInventory((v) => !v); setSel(null) }}
              style={{
                display: "flex", alignItems: "center", gap: 8, background: inventory ? "rgba(232,89,12,0.15)" : "transparent",
                border: `1px solid ${inventory ? "rgba(232,89,12,0.4)" : "rgba(255,255,255,0.13)"}`,
                color: inventory ? "#f5a06a" : "#cfc8c2", fontSize: 13.5, fontWeight: 600, padding: "10px 14px",
                borderRadius: 9, cursor: "pointer", fontFamily: "inherit",
              }}
            >
              Inventaire <span style={{ fontSize: 10, fontWeight: 700, background: "rgba(255,255,255,0.08)", color: MUTED, padding: "2px 5px", borderRadius: 4, letterSpacing: ".05em" }}>ADMIN</span>
            </button>
          ) : null}
          {canOperate && !inventory ? (
            <button
              onClick={openNewDeposit}
              style={{ display: "flex", alignItems: "center", gap: 8, background: ORANGE, border: "none", color: "#fff", fontSize: 13.5, fontWeight: 700, padding: "10px 17px", borderRadius: 9, cursor: "pointer", fontFamily: "inherit", boxShadow: "0 4px 14px rgba(232,89,12,0.32)" }}
            >+ Déposer</button>
          ) : null}
        </div>
      </div>

      {/* KPI strip */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 12, marginBottom: 15 }}>
        {[
          { l: "Valeur du stock", v: `${fmt(kpis.value)} septims` },
          { l: "Objets en stock", v: fmt(kpis.qty) },
          { l: "Références", v: fmt(kpis.refs) },
          { l: "Coffres", v: fmt(kpis.chests) },
        ].map((k) => (
          <div key={k.l} style={{ background: CARD, border: BORDER, borderRadius: 12, padding: "15px 17px" }}>
            <div style={{ color: MUTED, fontSize: 11.5, textTransform: "uppercase", letterSpacing: ".06em", fontWeight: 600 }}>{k.l}</div>
            <div style={{ color: TEXT, fontSize: 25, fontWeight: 700, marginTop: 6 }}>{k.v}</div>
          </div>
        ))}
      </div>

      {/* inventory banner */}
      {inventory ? (
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", background: "rgba(232,89,12,0.1)", border: "1px solid rgba(232,89,12,0.32)", borderRadius: 11, padding: "12px 16px", marginBottom: 15 }}>
          <div style={{ color: "#f0d8c4", fontSize: 13.5 }}>
            <b style={{ color: TEXT }}>Mode inventaire</b> — saisis les quantités comptées en jeu.{" "}
            <span style={{ color: "#f5a06a", fontWeight: 700 }}>{ecartCount} écart(s)</span> détecté(s).
          </div>
          <button onClick={validateInventory} style={{ background: ORANGE, border: "none", color: "#fff", fontSize: 13, fontWeight: 700, padding: "9px 15px", borderRadius: 8, cursor: "pointer", fontFamily: "inherit" }}>
            Valider l&apos;inventaire
          </button>
        </div>
      ) : null}

      {/* toolbar */}
      <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 15 }}>
        <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Rechercher un objet…"
          style={{ background: CARD, border: "1px solid rgba(255,255,255,0.1)", borderRadius: 9, color: TEXT, fontSize: 13.5, padding: "9px 12px", width: 240, outline: "none", fontFamily: "inherit" }} />
        <FilterRow label="Famille" value={fam} onChange={setFam} options={famOpts} />
        <FilterRow label="Matériau" value={mat} onChange={setMat} options={matOpts} />
      </div>

      {/* table */}
      <div style={{ overflow: "auto", border: BORDER, borderRadius: 12, background: TABLE_BG }}>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13.5 }}>
          <thead>
            <tr style={{ background: HEAD_BG }}>
              <th onClick={() => toggleSort("name")} style={{ ...thStyle, textAlign: "left" }}>OBJET <span style={{ color: ORANGE, fontSize: 10 }}>{arrow("name")}</span></th>
              <th onClick={() => toggleSort("chest")} style={{ ...thStyle, textAlign: "left" }}>COFFRE <span style={{ color: ORANGE, fontSize: 10 }}>{arrow("chest")}</span></th>
              <th onClick={() => toggleSort("qty")} style={{ ...thStyle, textAlign: "right" }}>{inventory ? "SYSTÈME" : "QUANTITÉ"} <span style={{ color: ORANGE, fontSize: 10 }}>{arrow("qty")}</span></th>
              {inventory ? <th style={{ ...thStyle, textAlign: "right", color: ORANGE, cursor: "default" }}>COMPTÉ</th> : null}
              {inventory ? <th style={{ ...thStyle, textAlign: "right", cursor: "default" }}>ÉCART</th> : null}
              <th onClick={() => toggleSort("unit")} style={{ ...thStyle, textAlign: "right" }}>VAL. UNIT. <span style={{ color: ORANGE, fontSize: 10 }}>{arrow("unit")}</span></th>
              <th onClick={() => toggleSort("total")} style={{ ...thStyle, textAlign: "right" }}>VAL. TOTALE <span style={{ color: ORANGE, fontSize: 10 }}>{arrow("total")}</span></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => {
              const key = `${r.accountId}|${r.itemId}`
              const cVal = counted[key]
              const ecart = cVal !== undefined && cVal !== "" ? Number(cVal) - r.quantity : 0
              const selected = sel?.itemId === r.itemId && sel?.accountId === r.accountId
              return (
                <tr key={key} onClick={() => openRow(r)}
                  style={{ borderBottom: "1px solid rgba(255,255,255,0.05)", cursor: inventory ? "default" : "pointer", background: selected ? "rgba(232,89,12,0.08)" : "transparent" }}>
                  <td style={{ padding: "11px 16px" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 11 }}>
                      <div style={{ width: 31, height: 31, borderRadius: 8, background: r.color, display: "flex", alignItems: "center", justifyContent: "center", color: "#16110d", fontWeight: 800, fontSize: 13, flex: "none" }}>{initials(r.itemName)}</div>
                      <div style={{ lineHeight: 1.25 }}>
                        <div style={{ color: TEXT, fontWeight: 600 }}>{r.itemName}</div>
                        <div style={{ color: MUTED, fontSize: 12 }}>{[r.familyName, r.materialName].filter(Boolean).join(" · ") || "—"}</div>
                      </div>
                    </div>
                  </td>
                  <td style={{ padding: "11px 16px" }}><span style={{ background: "rgba(255,255,255,0.06)", color: "#cfc8c2", padding: "4px 9px", borderRadius: 6, fontSize: 12.5 }}>{r.accountName}</span></td>
                  <td style={inventory ? { ...tdNum, color: "#9a938c" } : tdNum}>{fmt(r.quantity)}</td>
                  {inventory ? (
                    <td style={{ padding: "6px 14px", textAlign: "right" }} onClick={(e) => e.stopPropagation()}>
                      <input value={cVal ?? ""} onChange={(e) => setCounted((p) => ({ ...p, [key]: e.target.value }))} type="number"
                        style={{ width: 74, background: INPUT_BG, border: `1px solid ${ecart !== 0 && cVal ? "rgba(232,89,12,0.6)" : "rgba(255,255,255,0.12)"}`, borderRadius: 7, color: TEXT, fontSize: 13, fontWeight: 700, textAlign: "right", padding: "6px 9px", outline: "none", fontFamily: "inherit", fontVariantNumeric: "tabular-nums" }} />
                    </td>
                  ) : null}
                  {inventory ? (
                    <td style={{ padding: "8px 14px", textAlign: "right" }}>
                      <span style={{ display: "inline-block", minWidth: 42, background: ecart === 0 ? "rgba(255,255,255,0.06)" : ecart > 0 ? "rgba(120,180,120,0.18)" : "rgba(232,89,12,0.18)", color: ecart === 0 ? MUTED : ecart > 0 ? "#88c088" : "#f5a06a", fontWeight: 700, fontSize: 12.5, padding: "3px 9px", borderRadius: 6, fontVariantNumeric: "tabular-nums" }}>
                        {cVal ? (ecart > 0 ? `+${ecart}` : ecart) : "—"}
                      </span>
                    </td>
                  ) : null}
                  <td style={{ ...tdNum, color: "#9a938c" }}>{fmt(r.unit)}</td>
                  <td style={{ ...tdNum, fontWeight: 700 }}>{fmt(r.total)}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
        <div style={{ padding: "11px 16px", color: "#6f6862", fontSize: 12.5, borderTop: "1px solid rgba(255,255,255,0.05)" }}>{rows.length} référence(s) affichée(s)</div>
      </div>

      {/* side panel (drawer) */}
      {sel && !inventory ? (
        <div style={{ position: "fixed", top: 0, right: 0, bottom: 0, width: 386, background: "#1a1613", borderLeft: "1px solid rgba(255,255,255,0.1)", boxShadow: "-26px 0 55px rgba(0,0,0,0.42)", display: "flex", flexDirection: "column", zIndex: 50, fontFamily: "system-ui" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "16px 18px", borderBottom: BORDER }}>
            <div style={{ color: MUTED, fontSize: 11.5, textTransform: "uppercase", letterSpacing: ".08em", fontWeight: 600 }}>Mouvement de stock</div>
            <button onClick={() => setSel(null)} style={{ background: "transparent", border: "none", color: MUTED, cursor: "pointer", fontSize: 18 }}>✕</button>
          </div>

          <div style={{ padding: 18, display: "flex", flexDirection: "column", gap: 14, overflow: "auto" }}>
            {sel.itemId ? (
              <div style={{ display: "flex", gap: 13, alignItems: "center" }}>
                <div style={{ width: 46, height: 46, borderRadius: 11, background: selItem?.familyColor ?? DEFAULT_CAT, display: "flex", alignItems: "center", justifyContent: "center", color: "#16110d", fontWeight: 800, fontSize: 19, flex: "none" }}>{initials(selRow?.itemName ?? "?")}</div>
                <div style={{ lineHeight: 1.3 }}>
                  <div style={{ color: TEXT, fontSize: 17, fontWeight: 700 }}>{selRow?.itemName}</div>
                  <div style={{ color: MUTED, fontSize: 12.5 }}>{[selItem?.familyName, selItem?.materialName].filter(Boolean).join(" · ") || "—"} · {selRow?.accountName}</div>
                </div>
              </div>
            ) : (
              <>
                <Field label="Objet"><Picker value={formItem} onChange={setFormItem} options={items.map((i) => ({ value: i.id, label: i.name }))} /></Field>
                <Field label="Coffre"><Picker value={formAccount} onChange={setFormAccount} options={accounts.map((a) => ({ value: a.id, label: a.name }))} /></Field>
              </>
            )}

            {selRow ? (
              <div style={{ display: "flex", gap: 9 }}>
                <Stat label="Quantité" value={fmt(selRow.quantity)} />
                <Stat label="Val. unit." value={fmt(selRow.unit)} />
                <Stat label="Total" value={fmt(selRow.total)} color={ORANGE} />
              </div>
            ) : null}

            <div style={{ display: "flex", background: INPUT_BG, border: "1px solid rgba(255,255,255,0.08)", borderRadius: 10, padding: 4, gap: 4 }}>
              {(["DEPOSIT", "WITHDRAW", "TRANSFER"] as Move[]).map((m) => (
                <button key={m} onClick={() => setMode(m)}
                  style={{ flex: 1, background: mode === m ? ORANGE : "transparent", color: mode === m ? "#fff" : "#cfc8c2", border: "none", borderRadius: 7, padding: 9, fontSize: 13, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>
                  {m === "DEPOSIT" ? "Déposer" : m === "WITHDRAW" ? "Retirer" : "Transférer"}
                </button>
              ))}
            </div>

            {mode === "TRANSFER" ? (
              <Field label="Vers le coffre">
                <Picker value={transferTo} onChange={setTransferTo} options={[{ value: "", label: "— choisir —" }, ...accounts.filter((a) => a.id !== selAccountId).map((a) => ({ value: a.id, label: a.name }))]} />
              </Field>
            ) : null}

            <Field label="Quantité">
              <input value={qty} onChange={(e) => setQty(e.target.value)} type="number" min={1} placeholder="0"
                style={{ width: "100%", background: INPUT_BG, border: "1px solid rgba(255,255,255,0.12)", borderRadius: 9, color: TEXT, fontSize: 18, fontWeight: 700, textAlign: "center", padding: 8, outline: "none", fontFamily: "inherit" }} />
              <div style={{ display: "flex", gap: 7, marginTop: 9 }}>
                {[1, 10, 100].map((d) => (<button key={d} onClick={() => setQty((q) => String((Number(q) || 0) + d))} style={quickBtn}>+{d}</button>))}
                {selRow ? <button onClick={() => setQty(String(selRow.quantity))} style={quickBtn}>Max</button> : null}
              </div>
            </Field>

            <Field label="Motif (optionnel)">
              <input value={motif} onChange={(e) => setMotif(e.target.value)} placeholder="Ex. forge d'une commande…"
                style={{ width: "100%", background: INPUT_BG, border: "1px solid rgba(255,255,255,0.1)", borderRadius: 9, color: TEXT, fontSize: 13, padding: "9px 12px", outline: "none", fontFamily: "inherit" }} />
            </Field>

            <button onClick={submitMove} disabled={!canOperate}
              style={{ width: "100%", background: ORANGE, border: "none", color: "#fff", fontSize: 14, fontWeight: 700, padding: 12, borderRadius: 10, cursor: canOperate ? "pointer" : "not-allowed", opacity: canOperate ? 1 : 0.5, fontFamily: "inherit", boxShadow: "0 5px 16px rgba(232,89,12,0.28)" }}>
              {mode === "DEPOSIT" ? "Déposer" : mode === "WITHDRAW" ? "Retirer" : "Transférer"}
            </button>

            {sel.itemId ? (
              <div style={{ borderTop: BORDER, paddingTop: 14 }}>
                <div style={{ color: MUTED, fontSize: 11.5, textTransform: "uppercase", letterSpacing: ".06em", fontWeight: 600, marginBottom: 10 }}>Derniers mouvements</div>
                {movements.filter((m) => m.itemId === sel.itemId).slice(0, 6).map((m) => (
                  <div key={m.id} style={{ display: "flex", alignItems: "center", gap: 11, padding: "9px 0", borderBottom: "1px solid rgba(255,255,255,0.05)" }}>
                    <span style={{ fontSize: 10.5, fontWeight: 700, color: ORANGE, border: `1px solid ${ORANGE}`, padding: "2px 7px", borderRadius: 5, flex: "none" }}>{m.type}</span>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ color: "#cfc8c2", fontSize: 12.5 }}>{m.fromAccountName ?? "création"} → {m.toAccountName ?? "sortie"}</div>
                      <div style={{ color: "#6f6862", fontSize: 11.5 }}>{new Date(m.createdAt).toLocaleString("fr-FR")}</div>
                    </div>
                    <span style={{ color: TEXT, fontWeight: 700, fontSize: 13.5 }}>{m.quantity}</span>
                  </div>
                ))}
              </div>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  )
}

const quickBtn: React.CSSProperties = {
  flex: 1, background: "#232120", border: "1px solid rgba(255,255,255,0.08)", borderRadius: 7,
  color: "#9a938c", fontSize: 12.5, padding: 6, cursor: "pointer", fontFamily: "inherit",
}

function FilterRow({ label, value, onChange, options }: {
  label: string
  value: string
  onChange: (v: string) => void
  options: { id: string; nom: string; count: number }[]
}) {
  if (options.length === 0) return null
  return (
    <div style={{ display: "flex", gap: 6, flexWrap: "wrap", alignItems: "center" }}>
      <span style={{ color: MUTED, fontSize: 11, textTransform: "uppercase", letterSpacing: ".05em", width: 64 }}>{label}</span>
      <button onClick={() => onChange("all")} style={chipStyle(value === "all")}>Tous</button>
      {options.map((o) => (
        <button key={o.id} onClick={() => onChange(o.id)} style={chipStyle(value === o.id)}>
          {o.nom}
          <span style={{ fontSize: 11, fontWeight: 700, background: "rgba(255,255,255,0.08)", color: MUTED, padding: "2px 6px", borderRadius: 5 }}>{o.count}</span>
        </button>
      ))}
    </div>
  )
}

function chipStyle(active: boolean): React.CSSProperties {
  return {
    display: "flex", alignItems: "center", gap: 7,
    background: active ? "rgba(232,89,12,0.15)" : "rgba(255,255,255,0.05)",
    color: active ? "#f5a06a" : "#cfc8c2", border: "none", borderRadius: 999,
    padding: "7px 13px", fontSize: 13, fontWeight: 600, cursor: "pointer", fontFamily: "inherit",
  }
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label style={{ display: "block" }}>
      <div style={{ color: MUTED, fontSize: 11.5, textTransform: "uppercase", letterSpacing: ".06em", fontWeight: 600, marginBottom: 8 }}>{label}</div>
      {children}
    </label>
  )
}

function Stat({ label, value, color }: { label: string; value: string; color?: string }) {
  return (
    <div style={{ flex: 1, background: INPUT_BG, border: BORDER, borderRadius: 9, padding: "10px 12px" }}>
      <div style={{ color: MUTED, fontSize: 11 }}>{label}</div>
      <div style={{ color: color ?? TEXT, fontSize: 17, fontWeight: 700, marginTop: 2 }}>{value}</div>
    </div>
  )
}

function Picker({ value, onChange, options }: { value: string; onChange: (v: string) => void; options: { value: string; label: string }[] }) {
  return (
    <select value={value} onChange={(e) => onChange(e.target.value)}
      style={{ width: "100%", background: INPUT_BG, border: "1px solid rgba(255,255,255,0.12)", borderRadius: 9, color: TEXT, fontSize: 14, fontWeight: 600, padding: "11px 13px", outline: "none", fontFamily: "inherit", cursor: "pointer", colorScheme: "dark" }}>
      {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
    </select>
  )
}
