"use client"

import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useSession } from "@/lib/session"

function DashboardContent() {
  const me = useSession()
  const displayName = me.user.inGameName ?? me.user.username

  return (
    <>
      <PageHeader title={`Bonjour, ${displayName}`} description="Vue d'ensemble de tes business." />

      <Card>
        <CardHeader>
          <CardTitle>Mes business</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          {me.memberships.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Tu n'appartiens à aucun business pour l'instant. Demande à un administrateur de t'ajouter.
            </p>
          ) : (
            me.memberships.map((m) => (
              <div
                key={m.businessId}
                className="flex items-center justify-between rounded-md border px-4 py-3"
              >
                <div>
                  <p className="font-medium">{m.businessNom}</p>
                  <p className="text-xs text-muted-foreground">{m.businessType}</p>
                </div>
                <Badge variant={m.role === "ADMIN" ? "default" : "outline"}>{m.role}</Badge>
              </div>
            ))
          )}
        </CardContent>
      </Card>
    </>
  )
}

export default function DashboardPage() {
  return (
    <AppShell>
      <DashboardContent />
    </AppShell>
  )
}
