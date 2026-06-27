"use client"

import { useCallback, useEffect, useState } from "react"
import { Trash2 } from "lucide-react"
import { toast } from "sonner"
import { UserAutocomplete, type UserSummary } from "@/components/admin/user-autocomplete"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { SelectField } from "@/components/ui/select-field"
import { api, ApiError } from "@/lib/api"
import { useCurrentBusiness } from "@/lib/current-business"
import type { Account, AccountKind } from "@/lib/ledger"
import { canAdminBusiness, type MembershipRole } from "@/lib/roles"
import { useSession } from "@/lib/session"

type MemberDto = { userId: string; username: string; role: MembershipRole; version: number }
type Defaults = { stockAccountId: string | null; coffreAccountId: string | null }
type TaxRate = { rate: number; validFrom: string | null }
type TaxHistory = { rate: number; validFrom: string; validTo: string | null }
type Logo = { dataUrl: string | null }

const MAX_LOGO_BYTES = 500 * 1024

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

function LogoSection({ businessId }: { businessId: string }) {
  const [dataUrl, setDataUrl] = useState<string | null>(null)

  const load = useCallback(() => {
    api<Logo>(`/api/businesses/${businessId}/logo`).then((l) => setDataUrl(l.dataUrl)).catch(fail)
  }, [businessId])
  useEffect(() => load(), [load])

  async function save(value: string | null) {
    try {
      const l = await api<Logo>(`/api/businesses/${businessId}/logo`, {
        method: "PUT",
        body: JSON.stringify({ dataUrl: value }),
      })
      setDataUrl(l.dataUrl)
      toast.success("Logo mis à jour")
    } catch (err) {
      fail(err)
    }
  }

  function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > MAX_LOGO_BYTES) {
      toast.error("Image trop lourde (max 500 Ko)")
      return
    }
    const reader = new FileReader()
    reader.onload = () => save(reader.result as string)
    reader.readAsDataURL(file)
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Logo</CardTitle>
      </CardHeader>
      <CardContent className="flex items-center gap-4">
        <div className="flex size-20 items-center justify-center overflow-hidden rounded-md border bg-muted">
          {dataUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={dataUrl} alt="Logo du business" className="size-full object-contain" />
          ) : (
            <span className="text-xs text-muted-foreground">Aucun</span>
          )}
        </div>
        <div className="flex flex-col gap-2">
          <input type="file" accept="image/*" onChange={onFile} className="text-sm" />
          {dataUrl ? (
            <Button variant="ghost" size="sm" onClick={() => save(null)}>
              <Trash2 data-icon="inline-start" />
              Retirer
            </Button>
          ) : null}
          <span className="text-xs text-muted-foreground">PNG/JPG, max 500 Ko.</span>
        </div>
      </CardContent>
    </Card>
  )
}

function TaxSection({ businessId }: { businessId: string }) {
  const [current, setCurrent] = useState<TaxRate | null>(null)
  const [history, setHistory] = useState<TaxHistory[]>([])
  const [pct, setPct] = useState("")

  const load = useCallback(() => {
    api<TaxRate>(`/api/businesses/${businessId}/tax-rate`).then(setCurrent).catch(fail)
    api<TaxHistory[]>(`/api/businesses/${businessId}/tax-rate/history`).then(setHistory).catch(fail)
  }, [businessId])
  useEffect(() => load(), [load])

  async function save() {
    const value = Number(pct)
    if (pct === "" || Number.isNaN(value) || value < 0 || value > 100) {
      toast.error("Taux entre 0 et 100 %")
      return
    }
    try {
      await api(`/api/businesses/${businessId}/tax-rate`, {
        method: "PUT",
        body: JSON.stringify({ rate: value / 100 }),
      })
      setPct("")
      toast.success("Taux mis à jour")
      load()
    } catch (err) {
      fail(err)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Taxe</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <p className="text-sm">
          Taux courant : <strong>{current ? `${(current.rate * 100).toFixed(2)} %` : "…"}</strong>
        </p>
        <div className="flex flex-wrap items-center gap-2">
          <Input type="number" min={0} max={100} step="1" className="w-28" placeholder="% prélevé" value={pct} onChange={(e) => setPct(e.target.value)} />
          <span className="text-sm text-muted-foreground">%</span>
          <Button onClick={save}>Définir</Button>
        </div>
        {history.length > 0 ? (
          <div className="flex flex-col gap-1 border-t pt-2">
            <span className="text-xs font-medium text-muted-foreground">Historique</span>
            {history.map((h, i) => (
              <div key={i} className="flex justify-between text-xs text-muted-foreground">
                <span>{(h.rate * 100).toFixed(2)} %</span>
                <span>
                  {new Date(h.validFrom).toLocaleDateString("fr-FR")} →{" "}
                  {h.validTo ? new Date(h.validTo).toLocaleDateString("fr-FR") : "en cours"}
                </span>
              </div>
            ))}
          </div>
        ) : null}
      </CardContent>
    </Card>
  )
}

const KIND_LABEL: Record<AccountKind, string> = { COFFRE: "Coffre", STOCK: "Stock", AUTRE: "Autre" }

function AccountsSection({ businessId }: { businessId: string }) {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [name, setName] = useState("")
  const [kind, setKind] = useState<AccountKind>("COFFRE")

  const load = useCallback(() => {
    api<Account[]>(`/api/businesses/${businessId}/accounts`).then(setAccounts).catch(fail)
  }, [businessId])
  useEffect(() => load(), [load])

  async function create() {
    if (!name.trim()) {
      toast.error("Nom du compte requis")
      return
    }
    try {
      await api<Account>(`/api/businesses/${businessId}/accounts`, {
        method: "POST",
        body: JSON.stringify({ name: name.trim(), kind }),
      })
      setName("")
      toast.success("Compte créé")
      load()
    } catch (err) {
      fail(err)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Coffres &amp; comptes</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center gap-2 border-b pb-3">
          <Input className="max-w-xs" placeholder="Nom (ex. Coffre principal)" value={name} onChange={(e) => setName(e.target.value)} />
          <SelectField
            value={kind}
            onChange={(v) => setKind(v as AccountKind)}
            options={[
              { value: "COFFRE", label: "Coffre (septims)" },
              { value: "STOCK", label: "Stock" },
              { value: "AUTRE", label: "Autre" },
            ]}
          />
          <Button onClick={create}>Créer</Button>
        </div>
        {accounts.length === 0 ? (
          <p className="text-sm text-muted-foreground">Aucun compte.</p>
        ) : (
          accounts.map((a) => (
            <div key={a.id} className="flex items-center justify-between">
              <span className="text-sm">{a.name}</span>
              <Badge variant="outline">{KIND_LABEL[a.kind]}</Badge>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  )
}

function DefaultsSection({ businessId }: { businessId: string }) {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [stockId, setStockId] = useState("")
  const [coffreId, setCoffreId] = useState("")

  const load = useCallback(() => {
    api<Account[]>(`/api/businesses/${businessId}/accounts`).then(setAccounts).catch(fail)
    api<Defaults>(`/api/businesses/${businessId}/defaults`).then((d) => {
      setStockId(d.stockAccountId ?? "")
      setCoffreId(d.coffreAccountId ?? "")
    }).catch(fail)
  }, [businessId])
  useEffect(() => load(), [load])

  const opts = (kind: Account["kind"]) =>
    [{ value: "", label: "— aucun —" }].concat(
      accounts.filter((a) => a.kind === kind).map((a) => ({ value: a.id, label: a.name })),
    )

  async function save() {
    try {
      await api(`/api/businesses/${businessId}/defaults`, {
        method: "PUT",
        body: JSON.stringify({ stockAccountId: stockId || null, coffreAccountId: coffreId || null }),
      })
      toast.success("Comptes par défaut enregistrés")
    } catch (err) {
      fail(err)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Comptes par défaut (caisse)</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <p className="text-sm text-muted-foreground">
          Utilisés par la caisse : la marchandise sort du compte stock, les septims entrent dans le coffre.
        </p>
        <label className="flex flex-col gap-1 text-sm">
          Compte stock
          <SelectField value={stockId} onChange={setStockId} options={opts("STOCK")} />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          Coffre
          <SelectField value={coffreId} onChange={setCoffreId} options={opts("COFFRE")} />
        </label>
        <Button className="self-start" onClick={save}>Enregistrer</Button>
      </CardContent>
    </Card>
  )
}

function MembersSection({ businessId }: { businessId: string }) {
  const [members, setMembers] = useState<MemberDto[] | null>(null)
  const [picked, setPicked] = useState<UserSummary | null>(null)
  const [acKey, setAcKey] = useState(0)
  const [role, setRole] = useState<MembershipRole>("MEMBRE")

  const load = useCallback(() => {
    api<MemberDto[]>(`/api/businesses/${businessId}/members`).then(setMembers).catch(fail)
  }, [businessId])
  useEffect(() => load(), [load])

  async function add() {
    if (!picked) {
      toast.error("Choisis un utilisateur")
      return
    }
    try {
      await api(`/api/businesses/${businessId}/members`, {
        method: "POST",
        body: JSON.stringify({ userId: picked.id, role, version: 0 }),
      })
      setPicked(null)
      setAcKey((k) => k + 1)
      toast.success("Membre ajouté")
      load()
    } catch (err) {
      onStale(err)
    }
  }

  // 409 = données modifiées entre-temps → toast + refetch isolé de la liste (pas de F5).
  function onStale(err: unknown) {
    if (err instanceof ApiError && err.status === 409) { toast.warning(err.message); load() } else fail(err)
  }

  async function changeRole(userId: string, newRole: MembershipRole, version: number) {
    try {
      await api(`/api/businesses/${businessId}/members`, {
        method: "POST",
        body: JSON.stringify({ userId, role: newRole, version }),
      })
      load()
    } catch (err) {
      onStale(err)
    }
  }

  async function remove(userId: string) {
    try {
      await api<void>(`/api/businesses/${businessId}/members/${userId}`, { method: "DELETE" })
      toast.success("Membre retiré")
      load()
    } catch (err) {
      fail(err)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Membres</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center gap-2 border-b pb-3">
          <UserAutocomplete key={acKey} onSelect={setPicked} />
          <SelectField
            value={role}
            onChange={(v) => setRole(v as MembershipRole)}
            options={[
              { value: "MEMBRE", label: "MEMBRE" },
              { value: "ADMIN", label: "ADMIN" },
            ]}
          />
          <Button onClick={add}>Recruter</Button>
        </div>

        {members === null ? (
          <p className="text-sm text-muted-foreground">Chargement…</p>
        ) : members.length === 0 ? (
          <p className="text-sm text-muted-foreground">Aucun membre.</p>
        ) : (
          members.map((m) => (
            <div key={m.userId} className="flex flex-wrap items-center justify-between gap-2">
              <span className="text-sm">{m.username}</span>
              <div className="flex items-center gap-2">
                <SelectField
                  value={m.role}
                  onChange={(v) => changeRole(m.userId, v as MembershipRole, m.version)}
                  options={[
                    { value: "MEMBRE", label: "MEMBRE" },
                    { value: "ADMIN", label: "ADMIN" },
                  ]}
                />
                <Button variant="ghost" size="icon" onClick={() => remove(m.userId)}>
                  <Trash2 />
                </Button>
              </div>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  )
}

export function BusinessConfig() {
  const me = useSession()
  const { currentId, current } = useCurrentBusiness()

  if (!currentId) {
    return <p className="text-sm text-muted-foreground">Sélectionne un business (en haut) à configurer.</p>
  }
  if (!canAdminBusiness(me, currentId)) {
    return <p className="text-sm text-destructive">Réservé à l'administrateur de ce business.</p>
  }

  return (
    <div className="flex flex-col gap-6">
      <p className="text-sm text-muted-foreground">
        Configuration de <strong>{current?.nom}</strong>.
      </p>
      <LogoSection businessId={currentId} />
      <TaxSection businessId={currentId} />
      <AccountsSection businessId={currentId} />
      <DefaultsSection businessId={currentId} />
      <MembersSection businessId={currentId} />
    </div>
  )
}
