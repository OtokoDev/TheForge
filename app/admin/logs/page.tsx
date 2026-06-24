import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatDateTime } from "@/lib/format"
import { requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

export default async function AdminLogsPage() {
  const session = await requireRole("ADMIN")
  const logs = await prisma.activityLog.findMany({
    take: 100,
    include: { user: true },
    orderBy: { createdAt: "desc" },
  })

  return (
    <AppShell user={session.user}>
      <PageHeader title="Logs activité" description="Actions sensibles enregistrées." />
      <Card>
        <CardHeader>
          <CardTitle>Dernières actions</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Date</TableHead>
                <TableHead>Utilisateur</TableHead>
                <TableHead>Action</TableHead>
                <TableHead>Détails</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {logs.map((log) => (
                <TableRow key={log.id}>
                  <TableCell>{formatDateTime(log.createdAt)}</TableCell>
                  <TableCell>{log.user.username}</TableCell>
                  <TableCell>{log.action}</TableCell>
                  <TableCell>{log.details}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </AppShell>
  )
}
