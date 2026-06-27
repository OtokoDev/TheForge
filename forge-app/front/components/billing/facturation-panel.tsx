"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { toast } from "sonner"
import { api, ApiError } from "@/lib/api"
import type { Facture, Session } from "@/lib/billing"
import type { Item } from "@/lib/catalog"
import { useCurrentBusiness } from "@/lib/current-business"
import type { StockRow } from "@/lib/ledger"
import { canOperateBusiness } from "@/lib/roles"
import { useSession } from "@/lib/session"
import { useShift } from "@/lib/shift"

const ORANGE = "#E8590C"
const GREEN = "#5BBF73"
const RED = "#ed8472"
const TEXT = "#F4F1EE"
const MUTED = "#8f8880"
const CARD = "#1c1a18"
const TABLE_BG = "#1a1816"
const HEAD_BG = "#221f1b"
const INPUT_BG = "#15110e"
const BORDER = "1px solid rgba(255,255,255,0.07)"

const DEFAULT_CAT = "#7d90a6"
const itemColor = (i?: Item | null) => i?.familyColor ?? DEFAULT_CAT
const fmt = (n: number) => n.toLocaleString("fr-FR")

// Valeurs distinctes (id+nom) d'une dimension parmi une liste d'items.
function dedupe(items: Item[], idKey: "familyId" | "materialId", nameKey: "familyName" | "materialName") {
  const m = new Map<string, string>()
  for (const i of items) {
    const id = i[idKey]
    const nom = i[nameKey]
    if (id && nom) m.set(id, nom)
  }
  return Array.from(m, ([id, nom]) => ({ id, nom }))
}
const isToday = (iso: string) => new Date(iso).toDateString() === new Date().toDateString()

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

type StatusFilter = "today" | "all" | "paid" | "unpaid"

export function FacturationPanel() {
  const me = useSession()
  const { currentId } = useCurrentBusiness()
  const { shift, refresh: refreshShift } = useShift()
  const canOperate = currentId ? canOperateBusiness(me, currentId) : false

  const [view, setView] = useState<"list" | "pos">("list")
  const [factures, setFactures] = useState<Facture[]>([])

  const loadFactures = useCallback(() => {
    if (!currentId) return
    api<Facture[]>(`/api/businesses/${currentId}/factures`).then(setFactures).catch(fail)
  }, [currentId])
  useEffect(() => loadFactures(), [loadFactures])

  if (!currentId) {
    return <p style={{ color: MUTED, fontSize: 14 }}>Sélectionne un business (en haut) pour facturer.</p>
  }

  async function openShift() {
    try { await api(`/api/businesses/${currentId}/sessions/open`, { method: "POST" }); refreshShift() } catch (e) { fail(e) }
  }
  async function closeShift() {
    try {
      const s = await api<Session>(`/api/businesses/${currentId}/sessions/close`, { method: "POST" })
      toast.success(`Service fermé — ${s.ordersCount} facture(s), CA ${fmt(s.totalSales)} septims`)
      refreshShift()
    } catch (e) { fail(e) }
  }

  return view === "pos" ? (
    <Pos
      businessId={currentId}
      canOperate={canOperate}
      onBack={() => setView("list")}
      onEmitted={() => { loadFactures(); setView("list") }}
    />
  ) : (
    <FactureList
      businessId={currentId}
      factures={factures}
      shiftOpen={!!shift?.open}
      shiftSince={shift?.session?.openedAt ?? null}
      canOperate={canOperate}
      onNew={() => setView("pos")}
      onOpenShift={openShift}
      onCloseShift={closeShift}
      onChange={loadFactures}
    />
  )
}

// ── Écran principal : table des factures ─────────────────────────────────────
function FactureList({ businessId, factures, shiftOpen, shiftSince, canOperate, onNew, onOpenShift, onCloseShift, onChange }: {
  businessId: string
  factures: Facture[]
  shiftOpen: boolean
  shiftSince: string | null
  canOperate: boolean
  onNew: () => void
  onOpenShift: () => void
  onCloseShift: () => void
  onChange: () => void
}) {
  const [query, setQuery] = useState("")
  const [status, setStatus] = useState<StatusFilter>("today")
  const [sort, setSort] = useState<{ key: "num" | "date" | "client" | "total"; dir: "asc" | "desc" }>({ key: "num", dir: "desc" })
  const [open, setOpen] = useState<string | null>(null)

  const kpis = useMemo(() => {
    const validated = factures.filter((f) => f.status === "VALIDEE")
    const todayV = validated.filter((f) => isToday(f.createdAt))
    const caJour = todayV.filter((f) => f.paid).reduce((s, f) => s + f.totalAmount, 0)
    const unpaid = validated.filter((f) => !f.paid)
    const panier = todayV.length ? Math.round(todayV.reduce((s, f) => s + f.totalAmount, 0) / todayV.length) : 0
    return { caJour, emisJour: todayV.length, nonPaye: unpaid.reduce((s, f) => s + f.totalAmount, 0), nonPayeCount: unpaid.length, panier }
  }, [factures])

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase()
    let r = factures.filter((f) => {
      if (status === "today") return isToday(f.createdAt)
      if (status === "paid") return f.status === "VALIDEE" && f.paid
      if (status === "unpaid") return f.status === "VALIDEE" && !f.paid
      return true
    })
    if (q) r = r.filter((f) =>
      (`#${f.numero} ${f.clientName ?? ""} ${f.lines.map((l) => l.itemName).join(" ")}`).toLowerCase().includes(q))
    const dir = sort.dir === "asc" ? 1 : -1
    return [...r].sort((a, b) => {
      switch (sort.key) {
        case "client": return (a.clientName ?? "").localeCompare(b.clientName ?? "") * dir
        case "total": return (a.totalAmount - b.totalAmount) * dir
        case "date": return a.createdAt.localeCompare(b.createdAt) * dir
        default: return (a.numero - b.numero) * dir
      }
    })
  }, [factures, query, status, sort])

  function toggleSort(key: typeof sort.key) {
    setSort((s) => (s.key === key ? { key, dir: s.dir === "asc" ? "desc" : "asc" } : { key, dir: "desc" }))
  }

  async function validate(id: string, paid: boolean) {
    try { await api(`/api/businesses/${businessId}/factures/${id}/validate`, { method: "POST", body: JSON.stringify({ paid }) }); toast.success("Facture émise"); onChange() } catch (e) { fail(e) }
  }
  async function encaisser(id: string) {
    try { await api(`/api/businesses/${businessId}/factures/${id}/pay`, { method: "POST" }); toast.success("Encaissée"); onChange() } catch (e) { fail(e) }
  }

  const th: React.CSSProperties = { color: MUTED, fontWeight: 600, fontSize: 12, letterSpacing: ".03em", padding: "13px 16px", borderBottom: BORDER, whiteSpace: "nowrap" }
  const statusChips: { id: StatusFilter; l: string }[] = [
    { id: "today", l: "Aujourd'hui" }, { id: "all", l: "Toutes" }, { id: "paid", l: "Payées" }, { id: "unpaid", l: "Non payées" },
  ]

  return (
    <div style={{ fontFamily: "system-ui,-apple-system,'Segoe UI',sans-serif" }}>
      {/* header */}
      <div style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", marginBottom: 15 }}>
        <div>
          <div style={{ color: TEXT, fontSize: 24, fontWeight: 700 }}>Facturation</div>
          <div style={{ color: MUTED, fontSize: 13.5, marginTop: 3 }}>Émission directe · {kpis.emisJour} factures aujourd&apos;hui</div>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          {canOperate ? (
            shiftOpen ? (
              <div style={{ display: "flex", alignItems: "center", gap: 10, background: CARD, border: "1px solid rgba(91,191,115,0.3)", borderRadius: 999, padding: "7px 8px 7px 14px" }}>
                <span style={{ display: "flex", alignItems: "center", gap: 7, color: "#7fd398", fontSize: 13, fontWeight: 600 }}>
                  <span style={{ width: 8, height: 8, borderRadius: 999, background: GREEN, boxShadow: `0 0 8px ${GREEN}` }} />
                  Service ouvert
                </span>
                {shiftSince ? <span style={{ color: "#6f6862", fontSize: 12 }}>· {new Date(shiftSince).toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}</span> : null}
                <button onClick={onCloseShift} style={{ background: "rgba(255,255,255,0.06)", border: "none", color: "#cfc8c2", fontSize: 12, fontWeight: 600, padding: "5px 10px", borderRadius: 999, cursor: "pointer", fontFamily: "inherit" }}>Fermer</button>
              </div>
            ) : (
              <button onClick={onOpenShift} style={{ background: "transparent", border: "1px solid rgba(255,255,255,0.13)", color: "#cfc8c2", fontSize: 13, fontWeight: 600, padding: "8px 14px", borderRadius: 999, cursor: "pointer", fontFamily: "inherit" }}>Ouvrir le service</button>
            )
          ) : null}
          {canOperate ? (
            <button onClick={onNew} style={{ display: "flex", alignItems: "center", gap: 8, background: ORANGE, border: "none", color: "#fff", fontSize: 13.5, fontWeight: 700, padding: "10px 17px", borderRadius: 9, cursor: "pointer", fontFamily: "inherit", boxShadow: "0 4px 14px rgba(232,89,12,0.32)" }}>+ Nouvelle facture</button>
          ) : null}
        </div>
      </div>

      {/* KPIs */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 12, marginBottom: 15 }}>
        <Kpi label="Encaissé aujourd'hui" value={`${fmt(kpis.caJour)} septims`} />
        <Kpi label="Factures émises (jour)" value={fmt(kpis.emisJour)} />
        <Kpi label="Non payé (à crédit)" value={`${fmt(kpis.nonPaye)}`} sub={`· ${kpis.nonPayeCount} fact.`} color={RED} />
        <Kpi label="Panier moyen" value={fmt(kpis.panier)} />
      </div>

      {/* toolbar */}
      <div style={{ display: "flex", alignItems: "center", gap: 11, flexWrap: "wrap", marginBottom: 15 }}>
        <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="N° de facture, client, article…"
          style={{ background: CARD, border: "1px solid rgba(255,255,255,0.1)", borderRadius: 9, color: TEXT, fontSize: 13.5, padding: "9px 12px", width: 280, outline: "none", fontFamily: "inherit" }} />
        {statusChips.map((c) => (
          <button key={c.id} onClick={() => setStatus(c.id)}
            style={{ background: status === c.id ? ORANGE : "#1f1d1b", color: status === c.id ? "#fff" : "#cfc8c2", border: status === c.id ? `1px solid ${ORANGE}` : "1px solid rgba(255,255,255,0.08)", borderRadius: 999, padding: "7px 14px", fontSize: 13, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>
            {c.l}
          </button>
        ))}
      </div>

      {/* table */}
      <div style={{ overflow: "auto", border: BORDER, borderRadius: 12, background: TABLE_BG }}>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13.5 }}>
          <thead>
            <tr style={{ background: HEAD_BG }}>
              <th onClick={() => toggleSort("num")} style={{ ...th, textAlign: "left", cursor: "pointer" }}>N°</th>
              <th onClick={() => toggleSort("date")} style={{ ...th, textAlign: "left", cursor: "pointer" }}>DATE</th>
              <th onClick={() => toggleSort("client")} style={{ ...th, textAlign: "left", cursor: "pointer" }}>CLIENT</th>
              <th style={{ ...th, textAlign: "left" }}>ARTICLES</th>
              <th style={{ ...th, textAlign: "center" }}>STATUT</th>
              <th onClick={() => toggleSort("total")} style={{ ...th, textAlign: "right", cursor: "pointer" }}>TOTAL</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((f) => {
              const statut = f.status === "BROUILLON" ? "Brouillon" : f.paid ? "Payé" : "Non payé"
              const sColor = f.status === "BROUILLON" ? MUTED : f.paid ? "#7fd398" : RED
              const sBg = f.status === "BROUILLON" ? "rgba(255,255,255,0.06)" : f.paid ? "rgba(91,191,115,0.13)" : "rgba(229,96,77,0.13)"
              const articles = f.lines.map((l) => `${l.quantity}× ${l.itemName}`).join(", ")
              return (
                <>
                  <tr key={f.id} onClick={() => setOpen((o) => (o === f.id ? null : f.id))}
                    style={{ borderBottom: BORDER, cursor: "pointer", background: open === f.id ? "rgba(255,255,255,0.03)" : "transparent" }}>
                    <td style={{ padding: "11px 16px", color: TEXT, fontWeight: 600, fontVariantNumeric: "tabular-nums" }}>#{String(f.numero).padStart(4, "0")}</td>
                    <td style={{ padding: "11px 16px", color: "#9a938c" }}>{new Date(f.createdAt).toLocaleString("fr-FR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" })}</td>
                    <td style={{ padding: "11px 16px", color: "#e7e1db" }}>{f.clientName ?? "Client de passage"}</td>
                    <td style={{ padding: "11px 16px", color: "#9a938c", maxWidth: 280, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{articles}</td>
                    <td style={{ padding: "11px 16px", textAlign: "center" }}><span style={{ fontSize: 12, fontWeight: 700, color: sColor, background: sBg, padding: "4px 10px", borderRadius: 999 }}>{statut}</span></td>
                    <td style={{ padding: "11px 16px", textAlign: "right", color: TEXT, fontWeight: 700, fontVariantNumeric: "tabular-nums" }}>{fmt(f.totalAmount)}</td>
                  </tr>
                  {open === f.id ? (
                    <tr key={`${f.id}-d`}>
                      <td colSpan={6} style={{ background: "#181513", padding: "12px 16px", borderBottom: BORDER }}>
                        <div style={{ display: "flex", flexWrap: "wrap", gap: 18, alignItems: "center", justifyContent: "space-between" }}>
                          <div style={{ color: "#cfc8c2", fontSize: 13 }}>
                            {f.lines.map((l) => `${l.quantity}× ${l.itemName} (${fmt(l.lineTotal || l.unitPrice * l.quantity)})`).join(" · ")}
                            {f.status === "VALIDEE" ? <span style={{ color: MUTED }}> — bénéf. {fmt(f.totalProfit)} · part forge {fmt(f.businessShare)} · part forgeron {fmt(f.workerShare)} septims</span> : null}
                          </div>
                          {canOperate ? (
                            <div style={{ display: "flex", gap: 8 }} onClick={(e) => e.stopPropagation()}>
                              {f.status === "BROUILLON" ? (
                                <>
                                  <button onClick={() => validate(f.id, true)} style={btnPrimary}>Émettre &amp; encaisser</button>
                                  <button onClick={() => validate(f.id, false)} style={btnGhost}>Émettre à crédit</button>
                                </>
                              ) : !f.paid ? (
                                <button onClick={() => encaisser(f.id)} style={btnPrimary}>Encaisser</button>
                              ) : null}
                            </div>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  ) : null}
                </>
              )
            })}
          </tbody>
        </table>
        <div style={{ padding: "11px 16px", color: "#6f6862", fontSize: 12.5, borderTop: "1px solid rgba(255,255,255,0.05)" }}>{rows.length} factures</div>
      </div>
    </div>
  )
}

// ── POS / Caisse ─────────────────────────────────────────────────────────────
function Pos({ businessId, canOperate, onBack, onEmitted }: {
  businessId: string
  canOperate: boolean
  onBack: () => void
  onEmitted: () => void
}) {
  const [items, setItems] = useState<Item[]>([])
  const [prices, setPrices] = useState<Map<string, number>>(new Map())
  const [stockQty, setStockQty] = useState<Map<string, number>>(new Map())
  const [query, setQuery] = useState("")
  const [fam, setFam] = useState("all")
  const [mat, setMat] = useState("all")
  const [cart, setCart] = useState<Record<string, number>>({})
  const [client, setClient] = useState("")
  const [paid, setPaid] = useState(true)

  useEffect(() => {
    api<Item[]>("/api/catalog/items").then(setItems).catch(fail)
    api<{ itemId: string; prixRevente: number | null }[]>(`/api/businesses/${businessId}/products`)
      .then((rows) => setPrices(new Map(
        rows.filter((r) => r.prixRevente != null).map((r) => [r.itemId, r.prixRevente as number])
      ))).catch(fail)
    api<StockRow[]>(`/api/businesses/${businessId}/stock`).then((rows) => {
      const m = new Map<string, number>()
      for (const r of rows) m.set(r.itemId, (m.get(r.itemId) ?? 0) + r.quantity)
      setStockQty(m)
    }).catch(fail)
  }, [businessId])

  function stateOf(it: Item): { label: string; color: string; bg: string; sellable: boolean } {
    if ((stockQty.get(it.id) ?? 0) > 0) return { label: "Disponible", color: GREEN, bg: "rgba(91,191,115,0.13)", sellable: true }
    if (it.hasRecipe) return { label: "Fabricable", color: "#d9a441", bg: "rgba(217,164,65,0.13)", sellable: true }
    return { label: "Indisponible", color: "#E5604D", bg: "rgba(229,96,77,0.13)", sellable: false }
  }

  // Familles/matériaux présents parmi les articles vendables (pour les filtres).
  const sellable = useMemo(() => items.filter((i) => !i.system && prices.has(i.id)), [items, prices])
  const fams = useMemo(() => dedupe(sellable, "familyId", "familyName"), [sellable])
  const mats = useMemo(() => dedupe(sellable, "materialId", "materialName"), [sellable])
  const catalogue = useMemo(() => {
    const q = query.trim().toLowerCase()
    return sellable.filter((i) =>
      (fam === "all" || i.familyId === fam) &&
      (mat === "all" || i.materialId === mat) &&
      (q === "" || i.name.toLowerCase().includes(q)))
  }, [sellable, fam, mat, query])

  const itemById = useMemo(() => new Map(items.map((i) => [i.id, i])), [items])
  const lines = Object.entries(cart).map(([id, qty]) => ({ id, item: itemById.get(id), qty }))
  const total = lines.reduce((s, l) => s + (prices.get(l.id) ?? 0) * l.qty, 0)
  const count = lines.reduce((s, l) => s + l.qty, 0)

  function add(id: string) { setCart((c) => ({ ...c, [id]: (c[id] ?? 0) + 1 })) }
  function dec(id: string) { setCart((c) => { const n = { ...c }; n[id] = (n[id] ?? 0) - 1; if (n[id] <= 0) delete n[id]; return n }) }

  async function emit(asDraft: boolean) {
    if (Object.keys(cart).length === 0) { toast.error("Panier vide"); return }
    const body = { lines: lines.map((l) => ({ itemId: l.id, quantity: l.qty })), clientName: client || null }
    try {
      const created = await api<Facture>(`/api/businesses/${businessId}/factures`, { method: "POST", body: JSON.stringify(body) })
      if (!asDraft) {
        await api(`/api/businesses/${businessId}/factures/${created.id}/validate`, { method: "POST", body: JSON.stringify({ paid }) })
      }
      toast.success(asDraft ? "Brouillon enregistré" : "Facture émise")
      onEmitted()
    } catch (err) {
      fail(err)
    }
  }

  return (
    <div style={{ fontFamily: "system-ui,-apple-system,'Segoe UI',sans-serif" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 16 }}>
        <button onClick={onBack} style={{ background: "transparent", border: "none", color: MUTED, fontSize: 13, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>← Factures</button>
        <div style={{ color: TEXT, fontSize: 20, fontWeight: 700 }}>Nouvelle facture</div>
      </div>

      <div style={{ display: "flex", gap: 16, alignItems: "flex-start" }}>
        {/* catalogue */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Chercher un article du catalogue…"
            style={{ width: "100%", background: CARD, border: "1px solid rgba(255,255,255,0.1)", borderRadius: 9, color: TEXT, fontSize: 13.5, padding: "10px 12px", outline: "none", fontFamily: "inherit", marginBottom: 12 }} />
          <FilterRow label="Famille" value={fam} onChange={setFam} options={fams} />
          <FilterRow label="Matériau" value={mat} onChange={setMat} options={mats} />
          <div style={{ height: 6 }} />
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
            {catalogue.map((it) => {
              const st = stateOf(it)
              return (
                <button key={it.id} onClick={() => st.sellable && add(it.id)} disabled={!st.sellable}
                  style={{ textAlign: "left", background: CARD, border: BORDER, borderRadius: 11, padding: 13, cursor: st.sellable ? "pointer" : "not-allowed", opacity: st.sellable ? 1 : 0.5, fontFamily: "inherit", display: "flex", flexDirection: "column", gap: 9 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <div style={{ width: 34, height: 34, borderRadius: 9, background: itemColor(it), display: "flex", alignItems: "center", justifyContent: "center", color: "#16110d", fontWeight: 800, fontSize: 14, flex: "none" }}>{it.name.slice(0, 2).toUpperCase()}</div>
                    <div style={{ minWidth: 0, lineHeight: 1.25 }}>
                      <div style={{ color: TEXT, fontWeight: 600, fontSize: 13.5, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{it.name}</div>
                      <div style={{ color: MUTED, fontSize: 11.5 }}>{[it.familyName, it.materialName].filter(Boolean).join(" · ")}</div>
                    </div>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <span style={{ display: "inline-flex", alignItems: "center", gap: 6, fontSize: 11.5, fontWeight: 600, color: st.color, background: st.bg, padding: "3px 8px", borderRadius: 6 }}>
                      <span style={{ width: 6, height: 6, borderRadius: 999, background: st.color }} />{st.label}
                    </span>
                    <span style={{ color: TEXT, fontWeight: 700, fontSize: 14, fontVariantNumeric: "tabular-nums" }}>{fmt(prices.get(it.id) ?? 0)}</span>
                  </div>
                </button>
              )
            })}
          </div>
        </div>

        {/* cart */}
        <div style={{ width: 400, flex: "none", background: "#171513", border: BORDER, borderRadius: 12, display: "flex", flexDirection: "column" }}>
          <div style={{ padding: "16px 18px 10px" }}>
            <div style={{ color: MUTED, fontSize: 11.5, textTransform: "uppercase", letterSpacing: ".06em", fontWeight: 600, marginBottom: 8 }}>Client</div>
            <input value={client} onChange={(e) => setClient(e.target.value)} placeholder="Client de passage (optionnel)"
              style={{ width: "100%", background: CARD, border: "1px solid rgba(255,255,255,0.1)", borderRadius: 9, color: TEXT, fontSize: 13.5, padding: "10px 12px", outline: "none", fontFamily: "inherit" }} />
          </div>

          <div style={{ flex: 1, minHeight: 120, maxHeight: 360, overflow: "auto", padding: "4px 18px" }}>
            {lines.length === 0 ? (
              <div style={{ color: "#6f6862", fontSize: 13.5, textAlign: "center", padding: "40px 0" }}>Clique un article pour l&apos;ajouter</div>
            ) : (
              lines.map((l) => (
                <div key={l.id} style={{ display: "flex", alignItems: "center", gap: 11, padding: "11px 0", borderBottom: "1px solid rgba(255,255,255,0.06)" }}>
                  <div style={{ width: 32, height: 32, borderRadius: 8, background: itemColor(l.item), display: "flex", alignItems: "center", justifyContent: "center", color: "#16110d", fontWeight: 800, fontSize: 13, flex: "none" }}>{(l.item?.name ?? "?").slice(0, 2).toUpperCase()}</div>
                  <div style={{ flex: 1, minWidth: 0, lineHeight: 1.3 }}>
                    <div style={{ color: TEXT, fontWeight: 600, fontSize: 13.5, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{l.item?.name}</div>
                    <div style={{ color: MUTED, fontSize: 12 }}>{fmt(prices.get(l.id) ?? 0)} / u</div>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: 5, flex: "none" }}>
                    <button onClick={() => dec(l.id)} style={stepBtn}>−</button>
                    <span style={{ minWidth: 24, textAlign: "center", color: TEXT, fontWeight: 700 }}>{l.qty}</span>
                    <button onClick={() => add(l.id)} style={stepBtn}>+</button>
                  </div>
                  <div style={{ width: 74, textAlign: "right", color: TEXT, fontWeight: 700, fontSize: 13.5, fontVariantNumeric: "tabular-nums", flex: "none" }}>{fmt((prices.get(l.id) ?? 0) * l.qty)}</div>
                </div>
              ))
            )}
          </div>

          <div style={{ borderTop: "1px solid rgba(255,255,255,0.08)", padding: "16px 18px", background: "#19110d" }}>
            <div style={{ display: "flex", justifyContent: "space-between", color: "#9a938c", fontSize: 13, marginBottom: 6 }}><span>Sous-total · {count} articles</span><span style={{ fontVariantNumeric: "tabular-nums" }}>{fmt(total)}</span></div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: 14 }}>
              <span style={{ color: TEXT, fontSize: 15, fontWeight: 700 }}>Total</span>
              <span style={{ color: ORANGE, fontSize: 27, fontWeight: 800, fontVariantNumeric: "tabular-nums" }}>{fmt(total)} <span style={{ fontSize: 13, color: MUTED, fontWeight: 500 }}>septims</span></span>
            </div>
            <div style={{ display: "flex", background: INPUT_BG, border: "1px solid rgba(255,255,255,0.08)", borderRadius: 8, padding: 3, gap: 3, marginBottom: 12 }}>
              <button onClick={() => setPaid(true)} style={{ flex: 1, background: paid ? "#2f7a45" : "transparent", color: paid ? "#fff" : MUTED, border: "none", borderRadius: 6, padding: 8, fontSize: 12.5, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Payé</button>
              <button onClick={() => setPaid(false)} style={{ flex: 1, background: !paid ? "#9a4438" : "transparent", color: !paid ? "#fff" : MUTED, border: "none", borderRadius: 6, padding: 8, fontSize: 12.5, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Non payé</button>
            </div>
            <button onClick={() => emit(false)} disabled={!canOperate}
              style={{ width: "100%", background: ORANGE, border: "none", color: "#fff", fontSize: 15, fontWeight: 700, padding: 14, borderRadius: 11, cursor: canOperate ? "pointer" : "not-allowed", opacity: canOperate ? 1 : 0.5, fontFamily: "inherit", boxShadow: "0 6px 18px rgba(232,89,12,0.32)" }}>
              {paid ? `Émettre & encaisser · ${fmt(total)} septims` : `Émettre (non payé) · ${fmt(total)} septims`}
            </button>
            <button onClick={() => emit(true)} disabled={!canOperate}
              style={{ width: "100%", marginTop: 9, background: "transparent", border: "1px solid rgba(255,255,255,0.12)", color: "#cfc8c2", fontSize: 13, fontWeight: 600, padding: 10, borderRadius: 9, cursor: "pointer", fontFamily: "inherit" }}>Enregistrer comme brouillon</button>
          </div>
        </div>
      </div>
    </div>
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
    <div style={{ display: "flex", gap: 6, flexWrap: "wrap", alignItems: "center", marginBottom: 8 }}>
      <span style={{ color: MUTED, fontSize: 11, textTransform: "uppercase", letterSpacing: ".05em", width: 64 }}>{label}</span>
      <button onClick={() => onChange("all")} style={chip(value === "all")}>Tous</button>
      {options.map((o) => (
        <button key={o.id} onClick={() => onChange(o.id)} style={chip(value === o.id)}>{o.nom}</button>
      ))}
    </div>
  )
}

function Kpi({ label, value, sub, color }: { label: string; value: string; sub?: string; color?: string }) {
  return (
    <div style={{ background: CARD, border: BORDER, borderRadius: 12, padding: "15px 17px" }}>
      <div style={{ color: MUTED, fontSize: 11.5, textTransform: "uppercase", letterSpacing: ".06em", fontWeight: 600 }}>{label}</div>
      <div style={{ color: color ?? TEXT, fontSize: 25, fontWeight: 700, marginTop: 6 }}>{value} {sub ? <span style={{ fontSize: 13, color: MUTED, fontWeight: 500 }}>{sub}</span> : null}</div>
    </div>
  )
}

const stepBtn: React.CSSProperties = { width: 28, height: 28, background: "#232120", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 7, color: "#cfc8c2", fontSize: 16, cursor: "pointer" }
const btnPrimary: React.CSSProperties = { background: ORANGE, border: "none", color: "#fff", fontSize: 12.5, fontWeight: 700, padding: "7px 12px", borderRadius: 8, cursor: "pointer", fontFamily: "inherit" }
const btnGhost: React.CSSProperties = { background: "transparent", border: "1px solid rgba(255,255,255,0.13)", color: "#cfc8c2", fontSize: 12.5, fontWeight: 600, padding: "7px 12px", borderRadius: 8, cursor: "pointer", fontFamily: "inherit" }
