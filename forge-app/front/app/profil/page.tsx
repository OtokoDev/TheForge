"use client"

import { useState } from "react"
import { toast } from "sonner"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { api, ApiError } from "@/lib/api"
import { GLOBAL_ROLE_LABELS } from "@/lib/roles"
import { useSession } from "@/lib/session"

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between border-b py-2 last:border-b-0">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className="text-sm font-medium">{value}</span>
    </div>
  )
}

function ProfilContent() {
  const me = useSession()
  const u = me.user
  const [inGameName, setInGameName] = useState(u.inGameName ?? "")
  const [savedName, setSavedName] = useState(u.inGameName ?? "")
  const [saving, setSaving] = useState(false)
  const [webhooks, setWebhooks] = useState(u.webhooksEnabled)

  async function toggleWebhooks(next: boolean) {
    setWebhooks(next)
    try {
      await api("/api/me/webhooks", { method: "PUT", body: JSON.stringify({ enabled: next }) })
      toast.success("Préférence enregistrée")
    } catch (err) {
      setWebhooks(!next)
      toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
    }
  }

  async function save() {
    setSaving(true)
    try {
      await api("/api/me/in-game-name", {
        method: "PUT",
        body: JSON.stringify({ inGameName }),
      })
      toast.success("Pseudo mis à jour")
      setSavedName(inGameName)   // refresh local isolé, pas de rechargement de page
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <PageHeader title="Profil" description="Tes informations de compte." />

      <Card>
        <CardHeader>
          <CardTitle>Pseudo RP</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap items-center gap-2">
          <Input
            className="max-w-xs"
            placeholder="Nom en jeu"
            value={inGameName}
            onChange={(e) => setInGameName(e.target.value)}
          />
          <Button onClick={save} disabled={saving || inGameName === savedName}>
            Enregistrer
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Notifications</CardTitle>
        </CardHeader>
        <CardContent>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              className="size-4 accent-primary"
              checked={webhooks}
              onChange={(e) => toggleWebhooks(e.target.checked)}
            />
            Recevoir mes webhooks Discord (prise/fin de service, factures)
          </label>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Compte</CardTitle>
        </CardHeader>
        <CardContent>
          <Row label="Pseudo Discord" value={u.username} />
          <Row label="Nom en jeu" value={savedName || "—"} />
          <Row label="Discord ID" value={u.discordId} />
          <Row label="Rôle global" value={GLOBAL_ROLE_LABELS[u.globalRole]} />
          <Row label="Membre depuis" value={new Date(u.createdAt).toLocaleDateString("fr-FR")} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Appartenances</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2">
          {me.memberships.length === 0 ? (
            <p className="text-sm text-muted-foreground">Aucune appartenance.</p>
          ) : (
            me.memberships.map((m) => (
              <div key={m.businessId} className="flex items-center justify-between">
                <span className="text-sm">{m.businessNom}</span>
                <Badge variant={m.role === "ADMIN" ? "default" : "outline"}>{m.role}</Badge>
              </div>
            ))
          )}
        </CardContent>
      </Card>
    </>
  )
}

export default function ProfilPage() {
  return (
    <AppShell>
      <ProfilContent />
    </AppShell>
  )
}
