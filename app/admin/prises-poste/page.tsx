import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatDateTime, formatMoney } from "@/lib/format"
import { requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"
import { formatWorkedMinutes } from "@/lib/stats"

export default async function AdminShiftHistoryPage() {
  const session = await requireRole("ADMIN")
  const shifts = await prisma.shopSession.findMany({
    include: { user: true },
    orderBy: { openedAt: "desc" },
    take: 100,
  })
  const oldestOpenedAt = shifts.at(-1)?.openedAt
  const deliveredOrders = oldestOpenedAt
    ? await prisma.order.findMany({
        where: {
          status: "LIVREE",
          createdAt: { gte: oldestOpenedAt },
        },
        select: {
          userId: true,
          createdAt: true,
          totalCost: true,
          totalProfit: true,
          companyShare: true,
          smithShare: true,
        },
      })
    : []

  const shiftRows = shifts.map((shift) => {
    const closedAt = shift.closedAt ?? new Date()
    const orders = deliveredOrders.filter(
      (order) =>
        order.userId === shift.userId &&
        order.createdAt >= shift.openedAt &&
        order.createdAt <= closedAt,
    )
    const totalCost = orders.reduce((sum, order) => sum + order.totalCost, 0)
    const totalProfit = orders.reduce((sum, order) => sum + order.totalProfit, 0)
    const companyShare = orders.reduce((sum, order) => sum + order.companyShare, 0)
    const smithShare = orders.reduce((sum, order) => sum + order.smithShare, 0)

    return {
      shift,
      totalCost,
      totalProfit,
      companyShare,
      smithShare,
      cashDeposit: totalCost + companyShare,
    }
  })

  return (
    <AppShell user={session.user}>
      <PageHeader title="Prises de poste" description="Historique global des ouvertures de poste." />
      <Card>
        <CardHeader>
          <CardTitle>Historique global</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Forgeron</TableHead>
                <TableHead>Ouverture</TableHead>
                <TableHead>Fermeture</TableHead>
                <TableHead>Durée</TableHead>
                <TableHead>Factures</TableHead>
                <TableHead>CA</TableHead>
                <TableHead>Craft</TableHead>
                <TableHead>Bénéfice total</TableHead>
                <TableHead>Part forge</TableHead>
                <TableHead>Part forgeron</TableHead>
                <TableHead>Coffre</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {shiftRows.map(({ shift, totalCost, totalProfit, companyShare, smithShare, cashDeposit }) => (
                <TableRow key={shift.id}>
                  <TableCell>
                    <p className="font-medium">{shift.user.inGameName ?? shift.user.username}</p>
                    <p className="text-xs text-muted-foreground">{shift.user.username}</p>
                  </TableCell>
                  <TableCell>{formatDateTime(shift.openedAt)}</TableCell>
                  <TableCell>{shift.closedAt ? formatDateTime(shift.closedAt) : "En cours"}</TableCell>
                  <TableCell>
                    {formatWorkedMinutes(
                      Math.max(
                        0,
                        ((shift.closedAt ?? new Date()).getTime() - shift.openedAt.getTime()) / 60000,
                      ),
                    )}
                  </TableCell>
                  <TableCell>{shift.ordersCount}</TableCell>
                  <TableCell>{formatMoney(shift.totalSales)}</TableCell>
                  <TableCell>{formatMoney(totalCost)}</TableCell>
                  <TableCell>{formatMoney(totalProfit)}</TableCell>
                  <TableCell>{formatMoney(companyShare)}</TableCell>
                  <TableCell>{formatMoney(smithShare)}</TableCell>
                  <TableCell>{formatMoney(cashDeposit)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </AppShell>
  )
}
