"use client"

import { useEffect, useMemo, useState } from "react"
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts"
import { toast } from "sonner"
import { api, ApiError } from "@/lib/api"
import { exportCsv } from "@/lib/csv"
import { canStaffView } from "@/lib/roles"
import { useSession } from "@/lib/session"
import type { GlobalStats } from "@/lib/stats"

const ORANGE = "#E8590C"
const GREEN = "#5fa890"
const fmt = (n: number) => n.toLocaleString("fr-FR")
const tooltipStyle: React.CSSProperties = { background: "#1c1a18", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#F4F1EE", fontSize: 12 }

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

const PERIODS = [{ w: 4, l: "4 sem." }, { w: 12, l: "12 sem." }, { w: 26, l: "26 sem." }]

export function StaffStats() {
  const me = useSession()
  const [weeks, setWeeks] = useState(12)
  const [d, setD] = useState<GlobalStats | null>(null)

  const range = useMemo(() => ({
    from: new Date(Date.now() - weeks * 7 * 86400000).toISOString(),
    to: new Date().toISOString(),
  }), [weeks])

  useEffect(() => {
    if (!canStaffView(me)) return
    api<GlobalStats>(`/api/staff/stats/overview?from=${range.from}&to=${range.to}`).then(setD).catch(fail)
  }, [me, range])

  if (!canStaffView(me)) {
    return <p className="text-sm text-destructive">Réservé aux rôles STAFF / SYSTEM.</p>
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Vue staff — global</h1>
          <p className="text-sm text-muted-foreground">CA de l&apos;ensemble des business (lecture seule).</p>
        </div>
        <div className="flex gap-1">
          {PERIODS.map((p) => (
            <button key={p.w} onClick={() => setWeeks(p.w)}
              className={`rounded-md px-3 py-1.5 text-sm font-medium transition ${weeks === p.w ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted"}`}>
              {p.l}
            </button>
          ))}
        </div>
      </div>

      {!d ? <p className="text-sm text-muted-foreground">Chargement…</p> : (
        <>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
            <Kpi label="CA total" value={`${fmt(d.totalCa)} septims`} />
            <Kpi label="Bénéfice total" value={fmt(d.totalBenefice)} />
            <Kpi label="Item le plus vendu" value={d.topItems[0]?.nom ?? "—"} sub={d.topItems[0] ? `${fmt(d.topItems[0].valeur)} u.` : undefined} />
          </div>

          <ChartBox title="CA par semaine" onExport={() => exportCsv("ca-hebdo", ["Semaine", "CA", "Bénéfice"], d.serie.map((s) => [s.semaine, s.ca, s.benefice]))}>
            <BarChart data={d.serie} margin={{ left: 4, right: 8, top: 8 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
              <XAxis dataKey="semaine" tick={{ fontSize: 10, fill: "#8f8880" }} />
              <YAxis tick={{ fontSize: 11, fill: "#8f8880" }} width={48} />
              <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
              <Bar dataKey="ca" name="CA" fill={ORANGE} radius={[4, 4, 0, 0]} />
              <Bar dataKey="benefice" name="Bénéfice" fill={GREEN} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ChartBox>

          <div className="grid gap-4 md:grid-cols-2">
            <ChartBox title="CA par business" onExport={() => exportCsv("ca-par-business", ["Business", "CA"], d.parBusiness.map((b) => [b.nom, b.valeur]))}>
              <BarChart data={d.parBusiness} layout="vertical" margin={{ left: 8, right: 16 }}>
                <XAxis type="number" tick={{ fontSize: 11, fill: "#8f8880" }} />
                <YAxis type="category" dataKey="nom" width={120} tick={{ fontSize: 11, fill: "#cfc8c2" }} />
                <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
                <Bar dataKey="valeur" fill={ORANGE} radius={[0, 4, 4, 0]} />
              </BarChart>
            </ChartBox>
            <ChartBox title="Items les plus vendus (toutes forges)" onExport={() => exportCsv("top-items", ["Item", "Quantité"], d.topItems.map((t) => [t.nom, t.valeur]))}>
              <BarChart data={d.topItems.slice(0, 10)} layout="vertical" margin={{ left: 8, right: 16 }}>
                <XAxis type="number" tick={{ fontSize: 11, fill: "#8f8880" }} />
                <YAxis type="category" dataKey="nom" width={130} tick={{ fontSize: 11, fill: "#cfc8c2" }} />
                <Tooltip formatter={(v) => fmt(Number(v))} contentStyle={tooltipStyle} />
                <Bar dataKey="valeur" fill={GREEN} radius={[0, 4, 4, 0]} />
              </BarChart>
            </ChartBox>
          </div>
        </>
      )}
    </div>
  )
}

function Kpi({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="rounded-md border bg-card p-3">
      <div className="text-xs uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className="mt-1 truncate text-xl font-bold">{value}</div>
      {sub ? <div className="text-xs text-muted-foreground">{sub}</div> : null}
    </div>
  )
}

function ChartBox({ title, onExport, children }: { title: string; onExport?: () => void; children: React.ReactElement }) {
  return (
    <div className="rounded-md border bg-card p-3">
      <div className="mb-2 flex items-center justify-between">
        <div className="text-sm font-semibold">{title}</div>
        {onExport ? <button onClick={onExport} className="rounded border px-2 py-0.5 text-xs text-muted-foreground hover:bg-muted">Export CSV</button> : null}
      </div>
      <div className="h-72 w-full"><ResponsiveContainer width="100%" height="100%">{children}</ResponsiveContainer></div>
    </div>
  )
}
