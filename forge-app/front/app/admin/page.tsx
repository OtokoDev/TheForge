"use client"

import { useCallback, useEffect, useState } from "react"
import { toast } from "sonner"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { SelectField } from "@/components/ui/select-field"
import { api, ApiError } from "@/lib/api"
import type { BusinessType } from "@/lib/roles"

type BusinessDto = { id: string; nom: string; type: BusinessType; createdAt: string }

function fail(err: unknown) {
  toast.error(err instanceof ApiError ? err.message : "Erreur inattendue")
}

function AdminContent() {
  const [businesses, setBusinesses] = useState<BusinessDto[]>([])
  const [nom, setNom] = useState("")
  const [type, setType] = useState<BusinessType>("FORGE")

  const load = useCallback(() => {
    api<BusinessDto[]>("/api/businesses").then(setBusinesses).catch(fail)
  }, [])

  useEffect(() => load(), [load])

  async function createBusiness() {
    if (!nom.trim()) return
    try {
      await api<BusinessDto>("/api/businesses", {
        method: "POST",
        body: JSON.stringify({ nom: nom.trim(), type }),
      })
      setNom("")
      toast.success("Business créé")
      load()
    } catch (err) {
      fail(err)
    }
  }

  return (
    <>
      <PageHeader title="Administration" description="Création et liste des business." />

      <Card>
        <CardHeader>
          <CardTitle>Créer un business</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap items-center gap-2">
          <Input
            className="max-w-xs"
            placeholder="Nom du business"
            value={nom}
            onChange={(e) => setNom(e.target.value)}
          />
          <SelectField
            value={type}
            onChange={(v) => setType(v as BusinessType)}
            options={[
              { value: "FORGE", label: "FORGE" },
              { value: "COMPAGNIE", label: "COMPAGNIE" },
            ]}
          />
          <Button onClick={createBusiness}>Créer</Button>
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {businesses.map((b) => (
          <Card key={b.id}>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>{b.nom}</CardTitle>
                <Badge variant="outline">{b.type}</Badge>
              </div>
            </CardHeader>
          </Card>
        ))}
        {businesses.length === 0 ? <p className="text-sm text-muted-foreground">Aucun business.</p> : null}
      </div>

      <p className="text-sm text-muted-foreground">
        Membres, taxe et logo : sélectionne le business (en haut) puis ouvre <strong>Configuration</strong>.
      </p>
    </>
  )
}

export default function AdminPage() {
  return (
    <AppShell>
      <AdminContent />
    </AppShell>
  )
}
