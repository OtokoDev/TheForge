import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatDateTime } from "@/lib/format"
import { requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

export default async function AdminWebhooksPage() {
  const session = await requireRole("ADMIN")
  const logs = await prisma.webhookLog.findMany({
    take: 100,
    orderBy: { createdAt: "desc" },
  })

  return (
    <AppShell user={session.user}>
      <PageHeader title="Logs webhooks" description="Historique des envois Discord." />
      <Card>
        <CardHeader>
          <CardTitle>Discord</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Date</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Statut</TableHead>
                <TableHead>Erreur</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {logs.map((log) => (
                <TableRow key={log.id}>
                  <TableCell>{formatDateTime(log.createdAt)}</TableCell>
                  <TableCell>{log.type}</TableCell>
                  <TableCell>
                    <Badge
                      variant={log.success ? "secondary" : "destructive"}
                      className={log.success ? "border-emerald-500/40 bg-emerald-500/15" : ""}
                    >
                      {log.success ? "Succès" : "Échec"}
                    </Badge>
                  </TableCell>
                  <TableCell>{log.error ?? "—"}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </AppShell>
  )
}
