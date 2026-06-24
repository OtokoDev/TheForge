import Link from "next/link"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { StatusBadge } from "@/components/orders/status-badge"
import { buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatDateTime, formatMoney, formatOrderNumber } from "@/lib/format"
import { requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

export default async function CommandesPage() {
  const session = await requireRole("FORGERON")
  const orders = await prisma.order.findMany({
    include: { user: true, items: { include: { product: true } } },
    orderBy: { createdAt: "desc" },
  })

  return (
    <AppShell user={session.user}>
      <PageHeader title="Commandes" description="Suivi et traitement des commandes client." />
      <Card>
        <CardHeader>
          <CardTitle>Toutes les commandes</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>N°</TableHead>
                <TableHead>Client</TableHead>
                <TableHead>Articles</TableHead>
                <TableHead>Statut</TableHead>
                <TableHead>Total</TableHead>
                <TableHead>Date</TableHead>
                <TableHead className="text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {orders.map((order) => (
                <TableRow key={order.id}>
                  <TableCell>{formatOrderNumber(order.orderNumber)}</TableCell>
                  <TableCell>{order.user.username}</TableCell>
                  <TableCell>{order.items.reduce((sum, item) => sum + item.quantity, 0)}</TableCell>
                  <TableCell>
                    <StatusBadge status={order.status} />
                  </TableCell>
                  <TableCell>{formatMoney(order.totalPrice)}</TableCell>
                  <TableCell>{formatDateTime(order.createdAt)}</TableCell>
                  <TableCell className="text-right">
                    <Link
                      className={buttonVariants({ variant: "outline", size: "sm" })}
                      href={`/commandes/${order.id}`}
                    >
                      Ouvrir
                    </Link>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </AppShell>
  )
}
