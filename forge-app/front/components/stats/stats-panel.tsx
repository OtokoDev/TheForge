"use client"

import { useEffect, useMemo, useState } from "react"
import {
  Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts"
import { toast } from "sonner"
import { api, ApiError } from "@/lib/api"
import { exportCsv } from "@/lib/csv"
import { useCurrentBusiness } from "@/lib/current-business"
import { formatMoney } from "@/lib/format"
import type { ActivityStats, ClientsStats, CreancesStats, Forgerons, HeatCell, Overview, Products, StockStats } from "@/lib/stats"

const ORANGE = "#E8590C"
const GREEN = "#5fa890"
const RED = "#ed8472"
const COLORS = [ORANGE, GREEN, "#a288bd", "#d9a441", "#7d90a6", "#d6855a", "#6a8fb0", "#88a06f", "#cf8a5a", "#9ab0c8"]
const fmt = (n: number) => n.toLocaleString("fr-FR")

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

type Tab = "overview" | "products" | "forgerons" | "stock" | "activity" | "creances" | "clients"
const TABS: { key: Tab; label: string }[] = [
  { key: "overview", label: "Vue d'ensemble" },
  { key: "products", label: "Produits" },
  { key: "forgerons", label: "Forgerons" },
  { key: "stock", label: "Stock" },
  { key: "activity", label: "Activité" },
  { key: "creances", label: "Créances" },
  { key: "clients", label: "Clients" },
]
const PERIODS = [{ d: 7, l: "7 j" }, { d: 30, l: "30 j" }, { d: 90, l: "90 j" }]
const iso = (d: Date) => d.toISOString().slice(0, 10)

export function StatsPanel() {
  const { currentId } = useCurrentBusiness()
  const [tab, setTab] = useState<Tab>("overview")
  const [from, setFrom] = useState<Date>(() => new Date(Date.now() - 30 * 86400000))
  const [to, setTo] = useState<Date>(() => new Date())
  const [data, setData] = useState<unknown>(null)
  const [loading, setLoading] = useState(false)

  function preset(d: number) { setFrom(new Date(Date.now() - d * 86400000)); setTo(new Date()) }
  function month() { const n = new Date(); setFrom(new Date(n.getFullYear(), n.getMonth(), 1)); setTo(new Date()) }
  const range = useMemo(() => ({ from: from.toISOString(), to: to.toISOString() }), [from, to])

  useEffect(() => {
    if (!currentId) return
    setLoading(true)
    setData(null)
    api(`/api/businesses/${currentId}/stats/${tab}?from=${range.from}&to=${range.to}`)
      .then(setData).catch(fail).finally(() => setLoading(false))
  }, [currentId, tab, range])

  if (!currentId) {
    return <p className="text-sm text-muted-foreground">Sélectionne un business (en haut) pour voir les statistiques.</p>
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold tracking-tight">Statistiques</h1>
        <div className="flex flex-wrap items-center gap-1">
          {PERIODS.map((p) => (
            <button key={p.d} onClick={() => preset(p.d)}
              className="rounded-md px-3 py-1.5 text-sm font-medium text-muted-foreground transition hover:bg-muted">
              {p.l}
            </button>
          ))}
          <button onClick={month} className="rounded-md px-3 py-1.5 text-sm font-medium text-muted-foreground transition hover:bg-muted">Mois</button>
          <input type="date" value={iso(from)} max={iso(to)} onChange={(e) => e.target.value && setFrom(new Date(e.target.value))}
            className="rounded-md border bg-card px-2 py-1 text-sm" />
          <span className="text-muted-foreground">→</span>
          <input type="date" value={iso(to)} min={iso(from)} onChange={(e) => e.target.value && setTo(new Date(e.target.value + "T23:59:59"))}
            className="rounded-md border bg-card px-2 py-1 text-sm" />
        </div>
      </div>

      <div className="flex flex-wrap gap-1 border-b">
        {TABS.map((t) => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`-mb-px border-b-2 px-3 py-2 text-sm font-medium transition ${tab === t.key ? "border-primary text-foreground" : "border-transparent text-muted-foreground hover:text-foreground"}`}>
            {t.label}
          </button>
        ))}
      </div>

      {loading ? <p className="text-sm text-muted-foreground">Chargement…</p> : null}
      {!loading && data ? (
        tab === "overview" ? <OverviewView d={data as Overview} />
          : tab === "products" ? <ProductsView d={data as Products} />
            : tab === "forgerons" ? <ForgeronsView d={data as Forgerons} />
              : tab === "stock" ? <StockView d={data as StockStats} />
                : tab === "activity" ? <ActivityView d={data as ActivityStats} />
                  : tab === "creances" ? <CreancesView d={data as CreancesStats} />
                    : <ClientsView d={data as ClientsStats} />
      ) : null}
    </div>
  )
}

// ── Sous-vues ────────────────────────────────────────────────────────────────
function OverviewView({ d }: { d: Overview }) {
  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-6">
        <Kpi label="Encaissé" value={`${fmt(d.caEncaisse)}`} cur={d.caEncaisse} prev={d.caEncaissePrev} />
        <Kpi label="Bénéfice" value={`${fmt(d.benefice)}`} cur={d.benefice} prev={d.beneficePrev} />
        <Kpi label="Taux marge" value={`${(d.tauxMarge * 100).toFixed(1)} %`} />
        <Kpi label="Panier moyen" value={fmt(d.panierMoyen)} cur={d.panierMoyen} prev={d.panierMoyenPrev} />
        <Kpi label="Factures" value={String(d.nbFactures)} cur={d.nbFactures} prev={d.nbFacturesPrev} />
        <Kpi label="Impayé" value={fmt(d.impaye)} sub={`${d.impayeCount} fact.`} color={RED} />
      </div>

      <ChartBox title="CA & bénéfice par jour">
        <AreaChart data={d.serie} margin={{ left: 4, right: 8, top: 8, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
          <XAxis dataKey="jour" tick={{ fontSize: 11, fill: "#8f8880" }} />
          <YAxis tick={{ fontSize: 11, fill: "#8f8880" }} width={48} />
          <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
          <Area type="monotone" dataKey="ca" name="CA" stroke={ORANGE} fill={ORANGE} fillOpacity={0.18} />
          <Area type="monotone" dataKey="benefice" name="Bénéfice" stroke={GREEN} fill={GREEN} fillOpacity={0.18} />
        </AreaChart>
      </ChartBox>

      <div className="grid gap-4 md:grid-cols-2">
        <ChartBox title="Encaissé vs à crédit">
          <PieChart>
            <Pie data={[{ nom: "Encaissé", valeur: d.caEncaisse }, { nom: "À crédit", valeur: d.impaye }]}
              dataKey="valeur" nameKey="nom" innerRadius={50} outerRadius={80} label>
              <Cell fill={GREEN} /><Cell fill={RED} />
            </Pie>
            <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
          </PieChart>
        </ChartBox>
        <ChartBox title="Répartition bénéfice">
          <PieChart>
            <Pie data={[{ nom: "Part business", valeur: d.partBusiness }, { nom: "Part forgeron", valeur: d.partForgeron }]}
              dataKey="valeur" nameKey="nom" innerRadius={50} outerRadius={80} label>
              <Cell fill={ORANGE} /><Cell fill="#a288bd" />
            </Pie>
            <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
          </PieChart>
        </ChartBox>
      </div>
    </div>
  )
}

function ProductsView({ d }: { d: Products }) {
  const [metric, setMetric] = useState<"ca" | "marge" | "qte">("ca")
  const top = [...d.top].sort((a, b) => b[metric] - a[metric]).slice(0, 10)
  return (
    <div className="flex flex-col gap-4">
      <ChartBox title="Top produits" right={
        <div className="flex items-center gap-1">
          {(["ca", "marge", "qte"] as const).map((m) => (
            <button key={m} onClick={() => setMetric(m)}
              className={`rounded px-2 py-1 text-xs font-medium ${metric === m ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted"}`}>
              {m === "ca" ? "CA" : m === "marge" ? "Marge" : "Qté"}
            </button>
          ))}
          <CsvButton onClick={() => exportCsv("top-produits", ["Produit", "CA", "Marge", "Qté"], d.top.map((p) => [p.name, p.ca, p.marge, p.qte]))} />
        </div>
      }>
        <BarChart data={top} layout="vertical" margin={{ left: 8, right: 16 }}>
          <XAxis type="number" tick={{ fontSize: 11, fill: "#8f8880" }} />
          <YAxis type="category" dataKey="name" width={130} tick={{ fontSize: 11, fill: "#cfc8c2" }} />
          <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
          <Bar dataKey={metric} fill={ORANGE} radius={[0, 4, 4, 0]} />
        </BarChart>
      </ChartBox>

      <div className="grid gap-4 md:grid-cols-2">
        <DonutBox title="Ventes par famille" data={d.parFamille} />
        <DonutBox title="Ventes par matériau" data={d.parMateriau} />
      </div>

      <div className="rounded-md border">
        <div className="border-b px-4 py-2 text-sm font-semibold">⚠ Vendus à perte ({d.pertes.length})</div>
        <div className="flex flex-col divide-y">
          {d.pertes.length === 0 ? <p className="px-4 py-3 text-sm text-muted-foreground">Aucun produit vendu à perte.</p> : null}
          {d.pertes.map((p, i) => (
            <div key={i} className="flex items-center justify-between px-4 py-2 text-sm">
              <span>{p.name}</span>
              <span className="text-muted-foreground">revente <strong className="text-foreground">{fmt(p.prixRevente)}</strong> &lt; coût <strong style={{ color: RED }}>{fmt(p.cout)}</strong></span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function ForgeronsView({ d }: { d: Forgerons }) {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex justify-end">
        <CsvButton onClick={() => exportCsv("forgerons", ["Forgeron", "CA", "Bénéfice", "Factures", "Heures", "CA/h"],
          d.forgerons.map((f) => [f.username, f.ca, f.benefice, f.nbFactures, (f.minutesService / 60).toFixed(1), Math.round(f.caParHeure)]))} />
      </div>
      <div className="overflow-auto rounded-md border">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-left text-xs text-muted-foreground">
            <tr>
              <th className="px-3 py-2">Forgeron</th><th className="px-3 py-2 text-right">CA</th>
              <th className="px-3 py-2 text-right">Bénéfice</th><th className="px-3 py-2 text-right">Factures</th>
              <th className="px-3 py-2 text-right">Heures</th><th className="px-3 py-2 text-right">CA / h</th>
            </tr>
          </thead>
          <tbody>
            {d.forgerons.map((f) => (
              <tr key={f.userId} className="border-t">
                <td className="px-3 py-2 font-medium">{f.username}</td>
                <td className="px-3 py-2 text-right tabular-nums">{fmt(f.ca)}</td>
                <td className="px-3 py-2 text-right tabular-nums">{fmt(f.benefice)}</td>
                <td className="px-3 py-2 text-right tabular-nums">{f.nbFactures}</td>
                <td className="px-3 py-2 text-right tabular-nums">{(f.minutesService / 60).toFixed(1)}</td>
                <td className="px-3 py-2 text-right tabular-nums">{fmt(Math.round(f.caParHeure))}</td>
              </tr>
            ))}
            {d.forgerons.length === 0 ? <tr><td colSpan={6} className="px-3 py-3 text-muted-foreground">Aucune activité.</td></tr> : null}
          </tbody>
        </table>
      </div>
      <ChartBox title="CA par heure de service">
        <BarChart data={d.forgerons.map((f) => ({ name: f.username, caParHeure: Math.round(f.caParHeure) }))}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
          <XAxis dataKey="name" tick={{ fontSize: 11, fill: "#8f8880" }} />
          <YAxis tick={{ fontSize: 11, fill: "#8f8880" }} width={48} />
          <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
          <Bar dataKey="caParHeure" fill={GREEN} radius={[4, 4, 0, 0]} />
        </BarChart>
      </ChartBox>
    </div>
  )
}

function StockView({ d }: { d: StockStats }) {
  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-3 sm:grid-cols-2">
        <Kpi label="Valeur du stock (coût)" value={`${fmt(d.valeurStock)} septims`} />
        <Kpi label="Références en rupture/faible" value={String(d.ruptures.length)} color={d.ruptures.length ? RED : undefined} />
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <div className="rounded-md border">
          <div className="border-b px-4 py-2 text-sm font-semibold">Stock faible (≤ 5)</div>
          <div className="flex flex-col divide-y">
            {d.ruptures.length === 0 ? <p className="px-4 py-3 text-sm text-muted-foreground">Rien en rupture.</p> : null}
            {d.ruptures.map((r, i) => (
              <div key={i} className="flex items-center justify-between px-4 py-2 text-sm">
                <span>{r.nom}</span><span style={{ color: r.valeur <= 0 ? RED : undefined }}>{r.valeur}</span>
              </div>
            ))}
          </div>
        </div>
        <ChartBox title="Top matières consommées">
          <BarChart data={d.topConsommees} layout="vertical" margin={{ left: 8, right: 16 }}>
            <XAxis type="number" tick={{ fontSize: 11, fill: "#8f8880" }} />
            <YAxis type="category" dataKey="nom" width={130} tick={{ fontSize: 11, fill: "#cfc8c2" }} />
            <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
            <Bar dataKey="valeur" fill={ORANGE} radius={[0, 4, 4, 0]} />
          </BarChart>
        </ChartBox>
      </div>
    </div>
  )
}

const DOW = ["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"]
function ActivityView({ d }: { d: ActivityStats }) {
  const map = new Map<string, HeatCell>()
  d.heatmap.forEach((c) => map.set(`${c.dow}-${c.hour}`, c))
  const max = Math.max(1, ...d.heatmap.map((c) => c.ca))
  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-3 sm:grid-cols-3">
        <Kpi label="Sessions" value={String(d.sessions)} />
        <Kpi label="Durée moyenne" value={`${(d.dureeMoyenneMin / 60).toFixed(1)} h`} />
        <Kpi label="CA / session" value={fmt(d.caParSession)} />
      </div>
      <div className="overflow-auto rounded-md border bg-card p-3">
        <div className="mb-2 text-sm font-semibold">Quand vend-on ? (CA par jour × heure)</div>
        <div className="inline-block">
          <div className="flex">
            <div className="w-10" />
            {Array.from({ length: 24 }, (_, h) => <div key={h} className="w-6 text-center text-[9px] text-muted-foreground">{h}</div>)}
          </div>
          {DOW.map((day, dow) => (
            <div key={dow} className="flex items-center">
              <div className="w-10 text-xs text-muted-foreground">{day}</div>
              {Array.from({ length: 24 }, (_, h) => {
                const c = map.get(`${dow}-${h}`)
                const v = c ? c.ca : 0
                return <div key={h} title={c ? `${day} ${h}h — ${fmt(c.ca)} (${c.count} fact.)` : ""}
                  className="m-px h-6 w-6 rounded-sm" style={{ background: v ? `rgba(232,89,12,${0.15 + 0.85 * v / max})` : "rgba(255,255,255,0.04)" }} />
              })}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function CreancesView({ d }: { d: CreancesStats }) {
  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Kpi label="Total dû" value={fmt(d.totalDu)} color={d.totalDu ? RED : undefined} />
        <Kpi label="Crédité" value={fmt(d.totalCredit)} />
        <Kpi label="Payé" value={fmt(d.totalPaid)} />
        <Kpi label="Ratio payé" value={`${(d.ratioPaye * 100).toFixed(0)} %`} />
      </div>
      <div className="flex justify-end">
        <CsvButton onClick={() => exportCsv("creances-farmeurs", ["Farmeur", "Crédité", "Payé", "Reste dû"],
          d.topFarmers.map((f) => [f.username, f.credited, f.paid, f.remaining]))} />
      </div>
      <ChartBox title="Crédits vs paiements par jour">
        <AreaChart data={d.serie} margin={{ left: 4, right: 8, top: 8, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
          <XAxis dataKey="jour" tick={{ fontSize: 11, fill: "#8f8880" }} />
          <YAxis tick={{ fontSize: 11, fill: "#8f8880" }} width={48} />
          <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
          <Area type="monotone" dataKey="credit" name="Crédit" stroke={ORANGE} fill={ORANGE} fillOpacity={0.18} />
          <Area type="monotone" dataKey="paiement" name="Paiement" stroke={GREEN} fill={GREEN} fillOpacity={0.18} />
        </AreaChart>
      </ChartBox>
      <div className="overflow-auto rounded-md border">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-left text-xs text-muted-foreground">
            <tr><th className="px-3 py-2">Farmeur</th><th className="px-3 py-2 text-right">Crédité</th><th className="px-3 py-2 text-right">Payé</th><th className="px-3 py-2 text-right">Reste dû</th></tr>
          </thead>
          <tbody>
            {d.topFarmers.map((f, i) => (
              <tr key={i} className="border-t">
                <td className="px-3 py-2 font-medium">{f.username}</td>
                <td className="px-3 py-2 text-right tabular-nums">{fmt(f.credited)}</td>
                <td className="px-3 py-2 text-right tabular-nums">{fmt(f.paid)}</td>
                <td className="px-3 py-2 text-right tabular-nums" style={{ color: f.remaining ? RED : undefined }}>{fmt(f.remaining)}</td>
              </tr>
            ))}
            {d.topFarmers.length === 0 ? <tr><td colSpan={4} className="px-3 py-3 text-muted-foreground">Aucune créance.</td></tr> : null}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function ClientsView({ d }: { d: ClientsStats }) {
  return (
    <div className="flex flex-col gap-4">
      <ChartBox title="Top clients (CA)" right={<CsvButton onClick={() => exportCsv("top-clients", ["Client", "CA", "Factures", "Impayé"], d.top.map((c) => [c.nom, c.ca, c.nbFactures, c.impaye]))} />}>
        <BarChart data={d.top.map((c) => ({ name: c.nom, ca: c.ca }))} layout="vertical" margin={{ left: 8, right: 16 }}>
          <XAxis type="number" tick={{ fontSize: 11, fill: "#8f8880" }} />
          <YAxis type="category" dataKey="name" width={130} tick={{ fontSize: 11, fill: "#cfc8c2" }} />
          <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
          <Bar dataKey="ca" fill={ORANGE} radius={[0, 4, 4, 0]} />
        </BarChart>
      </ChartBox>
      <div className="rounded-md border">
        <div className="flex items-center justify-between border-b px-4 py-2">
          <span className="text-sm font-semibold">Débiteurs ({d.debiteurs.length})</span>
          <CsvButton onClick={() => exportCsv("debiteurs", ["Client", "Impayé", "Factures"], d.debiteurs.map((c) => [c.nom, c.impaye, c.nbFactures]))} />
        </div>
        <div className="flex flex-col divide-y">
          {d.debiteurs.length === 0 ? <p className="px-4 py-3 text-sm text-muted-foreground">Aucun impayé.</p> : null}
          {d.debiteurs.map((c, i) => (
            <div key={i} className="flex items-center justify-between px-4 py-2 text-sm">
              <span>{c.nom} <span className="text-muted-foreground">· {c.nbFactures} fact.</span></span>
              <span style={{ color: RED }}>{fmt(c.impaye)}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

// ── Bricks ───────────────────────────────────────────────────────────────────
const tooltipStyle: React.CSSProperties = { background: "#1c1a18", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#F4F1EE", fontSize: 12 }

function CsvButton({ onClick }: { onClick: () => void }) {
  return <button onClick={onClick} className="rounded border px-2 py-1 text-xs text-muted-foreground hover:bg-muted">Export CSV</button>
}

function Kpi({ label, value, sub, cur, prev, color }: { label: string; value: string; sub?: string; cur?: number; prev?: number; color?: string }) {
  let delta: number | null = null
  if (cur !== undefined && prev !== undefined) delta = prev > 0 ? ((cur - prev) / prev) * 100 : cur > 0 ? 100 : 0
  return (
    <div className="rounded-md border bg-card p-3">
      <div className="text-xs uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className="mt-1 text-xl font-bold" style={color ? { color } : undefined}>{value}</div>
      {sub ? <div className="text-xs text-muted-foreground">{sub}</div> : null}
      {delta !== null ? (
        <div className={`text-xs font-medium ${delta >= 0 ? "text-emerald-500" : "text-red-400"}`}>
          {delta >= 0 ? "▲" : "▼"} {Math.abs(delta).toFixed(0)} %
        </div>
      ) : null}
    </div>
  )
}

function ChartBox({ title, right, children }: { title: string; right?: React.ReactNode; children: React.ReactElement }) {
  return (
    <div className="rounded-md border bg-card p-3">
      <div className="mb-2 flex items-center justify-between">
        <div className="text-sm font-semibold">{title}</div>
        {right}
      </div>
      <div className="h-72 w-full">
        <ResponsiveContainer width="100%" height="100%">{children}</ResponsiveContainer>
      </div>
    </div>
  )
}

function DonutBox({ title, data }: { title: string; data: { nom: string; valeur: number }[] }) {
  return (
    <ChartBox title={title}>
      <PieChart>
        <Pie data={data} dataKey="valeur" nameKey="nom" innerRadius={50} outerRadius={80} label={(e: { name?: string }) => e.name ?? ""}>
          {data.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
        </Pie>
        <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
      </PieChart>
    </ChartBox>
  )
}
