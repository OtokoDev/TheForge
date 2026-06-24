import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatMoney } from "@/lib/format"
import { requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"
import { formatWorkedMinutes, workedMinutes } from "@/lib/stats"

export default async function AdminSmithsPage() {
  const session = await requireRole("ADMIN")
  const users = await prisma.user.findMany({
    where: { role: { in: ["FORGERON", "GERANT", "ADMIN"] } },
    include: {
      orders: { where: { status: "LIVREE" } },
      shopSessions: true,
    },
  })

  const rows = users
    .map((user) => {
      const salesCount = user.orders.length
      const totalSales = user.orders.reduce((sum, order) => sum + order.totalPrice, 0)
      const totalProfit = user.orders.reduce((sum, order) => sum + order.totalProfit, 0)
      const totalCompanyShare = user.orders.reduce((sum, order) => sum + order.companyShare, 0)
      const totalSmithShare = user.orders.reduce((sum, order) => sum + order.smithShare, 0)

      return {
        id: user.id,
        name: user.inGameName ?? user.username,
        discord: user.username,
        salesCount,
        totalSales,
        totalProfit,
        totalCompanyShare,
        totalSmithShare,
        minutes: workedMinutes(user.shopSessions),
      }
    })
    .sort((left, right) => right.totalSales - left.totalSales)

  return (
    <AppShell user={session.user}>
      <PageHeader title="Forgerons" description="Statistiques globales de tous les forgerons." />
      <Card>
        <CardHeader>
          <CardTitle>Performance des forgerons</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Pseudo IG</TableHead>
                <TableHead>Ventes</TableHead>
                <TableHead>CA</TableHead>
                <TableHead>Bénéfice total</TableHead>
                <TableHead>Part forge</TableHead>
                <TableHead>Part forgeron</TableHead>
                <TableHead>Heures</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>
                    <p className="font-medium">{row.name}</p>
                    <p className="text-xs text-muted-foreground">{row.discord}</p>
                  </TableCell>
                  <TableCell>{row.salesCount}</TableCell>
                  <TableCell>{formatMoney(row.totalSales)}</TableCell>
                  <TableCell>{formatMoney(row.totalProfit)}</TableCell>
                  <TableCell>{formatMoney(row.totalCompanyShare)}</TableCell>
                  <TableCell>{formatMoney(row.totalSmithShare)}</TableCell>
                  <TableCell>{formatWorkedMinutes(row.minutes)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
      <div className="grid gap-4 lg:grid-cols-3">
        <Ranking title="Top CA" rows={[...rows].sort((a, b) => b.totalSales - a.totalSales)} value="totalSales" />
        <Ranking
          title="Top part forgeron"
          rows={[...rows].sort((a, b) => b.totalSmithShare - a.totalSmithShare)}
          value="totalSmithShare"
        />
        <Ranking
          title="Top ventes"
          rows={[...rows].sort((a, b) => b.salesCount - a.salesCount)}
          value="salesCount"
        />
      </div>
    </AppShell>
  )
}

function Ranking({
  title,
  rows,
  value,
}: {
  title: string
  rows: { id: string; name: string; totalSales: number; totalSmithShare: number; salesCount: number }[]
  value: "totalSales" | "totalSmithShare" | "salesCount"
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {rows.slice(0, 5).map((row, index) => (
          <div key={row.id} className="flex justify-between gap-3 text-sm">
            <span>
              {index + 1}. {row.name}
            </span>
            <span className="text-muted-foreground">
              {value === "salesCount" ? row[value] : formatMoney(row[value])}
            </span>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
