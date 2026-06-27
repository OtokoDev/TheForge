"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { Trash2 } from "lucide-react"
import { toast } from "sonner"
import { UserAutocomplete, type UserSummary } from "@/components/admin/user-autocomplete"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { SelectField } from "@/components/ui/select-field"
import { api, ApiError } from "@/lib/api"
import type { Item } from "@/lib/catalog"
import { useCurrentBusiness } from "@/lib/current-business"
import { formatDateTime, formatMoney } from "@/lib/format"
import { canOperateBusiness } from "@/lib/roles"
import { useSession } from "@/lib/session"
import type { CreanceEntry, CreanceFarmer } from "@/lib/treasury"

type Defaults = { stockAccountId: string | null; coffreAccountId: string | null }
type Line = { itemId: string; quantity: number }

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

export function CreancesPanel() {
  const me = useSession()
  const { currentId } = useCurrentBusiness()
  const canOperate = currentId ? canOperateBusiness(me, currentId) : false

  const [farmers, setFarmers] = useState<CreanceFarmer[]>([])
  const [items, setItems] = useState<Item[]>([])
  const [defaults, setDefaults] = useState<Defaults>({ stockAccountId: null, coffreAccountId: null })
  const [openFarmer, setOpenFarmer] = useState<string | null>(null)
  const [entries, setEntries] = useState<CreanceEntry[]>([])

  // dépôt
  const [depFarmer, setDepFarmer] = useState<UserSummary | null>(null)
  const [depKey, setDepKey] = useState(0)
  const [lines, setLines] = useState<Line[]>([])
  const [depMotif, setDepMotif] = useState("")
  // paiement
  const [payFarmer, setPayFarmer] = useState<UserSummary | null>(null)
  const [payKey, setPayKey] = useState(1)
  const [amount, setAmount] = useState("")
  const [payMotif, setPayMotif] = useState("")

  const load = useCallback(() => {
    if (!currentId) return
    api<CreanceFarmer[]>(`/api/businesses/${currentId}/creances`).then(setFarmers).catch(fail)
    api<Item[]>("/api/catalog/items").then((rows) => setItems(rows.filter((i) => !i.system))).catch(fail)
    api<Defaults>(`/api/businesses/${currentId}/defaults`).then(setDefaults).catch(fail)
  }, [currentId])
  useEffect(() => load(), [load])

  const totalDue = useMemo(() => farmers.reduce((s, f) => s + f.remaining, 0), [farmers])

  function showEntries(farmerId: string) {
    if (openFarmer === farmerId) { setOpenFarmer(null); return }
    setOpenFarmer(farmerId)
    api<CreanceEntry[]>(`/api/businesses/${currentId}/creances/${farmerId}/entries`).then(setEntries).catch(fail)
  }

  async function deposit() {
    if (!depFarmer) { toast.error("Choisis un farmeur"); return }
    if (lines.length === 0 || lines.some((l) => !l.itemId || l.quantity <= 0)) { toast.error("Ajoute au moins une ligne valide"); return }
    if (!defaults.stockAccountId) { toast.error("Aucun compte stock par défaut (Configuration)"); return }
    try {
      await api(`/api/businesses/${currentId}/creances/deposit`, {
        method: "POST",
        body: JSON.stringify({ farmerUserId: depFarmer.id, lines, stockAccountId: defaults.stockAccountId, reference: depMotif || null }),
      })
      toast.success("Dépôt enregistré")
      setDepFarmer(null); setDepKey((k) => k + 2); setLines([]); setDepMotif("")
      load()
    } catch (err) { fail(err) }
  }

  async function payment() {
    if (!payFarmer) { toast.error("Choisis un farmeur"); return }
    const n = Number(amount)
    if (!Number.isFinite(n) || n <= 0) { toast.error("Montant invalide"); return }
    if (!defaults.coffreAccountId) { toast.error("Aucun coffre par défaut (Configuration)"); return }
    try {
      await api(`/api/businesses/${currentId}/creances/payment`, {
        method: "POST",
        body: JSON.stringify({ farmerUserId: payFarmer.id, amount: n, coffreAccountId: defaults.coffreAccountId, reference: payMotif || null }),
      })
      toast.success("Paiement enregistré")
      setPayFarmer(null); setPayKey((k) => k + 2); setAmount(""); setPayMotif("")
      load()
    } catch (err) { fail(err) }
  }

  if (!currentId) {
    return <p className="text-sm text-muted-foreground">Sélectionne un business (en haut) pour gérer les créances.</p>
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Créances</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Total dû aux farmeurs : <strong className="text-foreground">{formatMoney(totalDue)}</strong>
        </p>
      </div>

      {canOperate ? (
        <div className="grid gap-4 lg:grid-cols-2">
          <Card>
            <CardHeader><CardTitle>Déposer (rachat de matières)</CardTitle></CardHeader>
            <CardContent className="flex flex-col gap-3">
              <UserAutocomplete key={`dep-${depKey}`} onSelect={setDepFarmer} />
              {lines.map((line, i) => (
                <div key={i} className="flex flex-wrap items-center gap-2">
                  <SelectField value={line.itemId} onChange={(v) => setLines((p) => p.map((l, j) => (j === i ? { ...l, itemId: v } : l)))}
                    options={[{ value: "", label: "Objet…" }].concat(items.map((it) => ({ value: it.id, label: it.name })))} />
                  <Input type="number" min={1} className="w-24" value={line.quantity}
                    onChange={(e) => setLines((p) => p.map((l, j) => (j === i ? { ...l, quantity: Number(e.target.value) } : l)))} />
                  <Button variant="ghost" size="icon" onClick={() => setLines((p) => p.filter((_, j) => j !== i))}><Trash2 /></Button>
                </div>
              ))}
              <div>
                <Button variant="outline" size="sm" disabled={items.length === 0} onClick={() => setLines((p) => [...p, { itemId: items[0]?.id ?? "", quantity: 1 }])}>Ajouter une ligne</Button>
              </div>
              <Input placeholder="Motif (optionnel)" value={depMotif} onChange={(e) => setDepMotif(e.target.value)} />
              <Button className="self-start" onClick={deposit}>Déposer</Button>
              <p className="text-xs text-muted-foreground">Valorisé au coût ; marchandise entrée en stock ; crédite le farmeur.</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Payer un farmeur</CardTitle></CardHeader>
            <CardContent className="flex flex-col gap-3">
              <UserAutocomplete key={`pay-${payKey}`} onSelect={setPayFarmer} />
              <Input type="number" min={1} placeholder="Montant (septims)" value={amount} onChange={(e) => setAmount(e.target.value)} />
              <Input placeholder="Motif (optionnel)" value={payMotif} onChange={(e) => setPayMotif(e.target.value)} />
              <Button className="self-start" onClick={payment}>Payer</Button>
              <p className="text-xs text-muted-foreground">Septims sortis du coffre par défaut.</p>
            </CardContent>
          </Card>
        </div>
      ) : null}

      <Card>
        <CardHeader><CardTitle>Farmeurs</CardTitle></CardHeader>
        <CardContent className="flex flex-col gap-2">
          {farmers.length === 0 ? <p className="text-sm text-muted-foreground">Aucune créance.</p> : null}
          {farmers.map((f) => (
            <div key={f.farmerUserId} className="rounded-md border">
              <button onClick={() => showEntries(f.farmerUserId)} className="flex w-full flex-wrap items-center justify-between gap-2 px-3 py-2 text-left hover:bg-muted/50">
                <span className="font-medium">{f.farmerUsername}</span>
                <span className="flex items-center gap-3 text-sm">
                  <span className="text-muted-foreground">crédité {formatMoney(f.totalCredit)}</span>
                  <span className="text-muted-foreground">payé {formatMoney(f.totalPaid)}</span>
                  <Badge variant={f.remaining > 0 ? "destructive" : "secondary"}>reste {formatMoney(f.remaining)}</Badge>
                </span>
              </button>
              {openFarmer === f.farmerUserId ? (
                <div className="border-t px-3 py-2">
                  {entries.length === 0 ? <p className="text-xs text-muted-foreground">Aucune entrée.</p> : null}
                  {entries.map((e, i) => (
                    <div key={i} className="flex flex-wrap items-center justify-between gap-2 py-1 text-sm">
                      <span className="flex items-center gap-2">
                        <Badge variant={e.type === "CREDIT" ? "outline" : "secondary"}>{e.type === "CREDIT" ? "Dépôt" : "Paiement"}</Badge>
                        <span className="text-muted-foreground">{e.reference ?? "—"}</span>
                      </span>
                      <span className="flex items-center gap-3 text-muted-foreground">
                        <span>{formatMoney(e.amount)}</span>
                        <span>{e.username}</span>
                        <span>{formatDateTime(e.createdAt)}</span>
                      </span>
                    </div>
                  ))}
                </div>
              ) : null}
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
