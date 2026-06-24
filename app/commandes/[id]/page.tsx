import { notFound } from "next/navigation"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { OrderStatusForm } from "@/components/orders/order-status-form"
import { StatusBadge } from "@/components/orders/status-badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatDateTime, formatMoney, formatOrderNumber } from "@/lib/format"
import { requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

const itemSourceLabels = {
  MADE_NOW: "Fabriqué",
  FROM_STOCK: "Stock",
}

export default async function CommandeDetailPage({ params }: { params: { id: string } }) {
  const session = await requireRole("FORGERON")
  const order = await prisma.order.findUnique({
    where: { id: params.id },
    include: { user: true, items: { include: { product: true } } },
  })
  if (!order) notFound()

  return (
    <AppShell user={session.user}>
      <PageHeader
        title={`Commande ${formatOrderNumber(order.orderNumber)}`}
        description={`Créée le ${formatDateTime(order.createdAt)} par ${order.user.username}`}
      />
      <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Articles</CardTitle>
            <StatusBadge status={order.status} />
          </CardHeader>
          <CardContent className="flex flex-col gap-5">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Produit</TableHead>
                  <TableHead>Quantité</TableHead>
                  <TableHead>Prix unitaire</TableHead>
                  <TableHead className="text-right">Total</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {order.items.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell>
                      <p className="font-medium">{item.product.name}</p>
                      <p className="text-xs text-muted-foreground">
                        {itemSourceLabels[item.itemSource]}
                      </p>
                      {item.itemSource === "FROM_STOCK" ? (
                        <p className="text-xs text-muted-foreground">
                          Stock avant {item.stockBefore ?? "?"}, après {item.stockAfter ?? "?"}
                        </p>
                      ) : null}
                    </TableCell>
                    <TableCell>{item.quantity}</TableCell>
                    <TableCell>{formatMoney(item.unitPrice)}</TableCell>
                    <TableCell className="text-right">
                      {formatMoney(item.unitPrice * item.quantity)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <div className="grid gap-3 rounded-md border p-4 sm:grid-cols-2 xl:grid-cols-5">
              <div>
                <p className="text-xs text-muted-foreground">Total</p>
                <p className="text-xl font-semibold">{formatMoney(order.totalPrice)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Prix de craft</p>
                <p className="text-xl font-semibold">{formatMoney(order.totalCost)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Bénéfice total</p>
                <p className="text-xl font-semibold">{formatMoney(order.totalProfit)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Part forge</p>
                <p className="text-xl font-semibold">{formatMoney(order.companyShare)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Part forgeron</p>
                <p className="text-xl font-semibold">{formatMoney(order.smithShare)}</p>
              </div>
            </div>
            <div>
              <p className="text-sm font-medium">Note client</p>
              <p className="mt-2 rounded-md border bg-muted/30 p-3 text-sm text-muted-foreground">
                {order.clientNote || "Aucune note client."}
              </p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Traitement</CardTitle>
          </CardHeader>
          <CardContent>
            <OrderStatusForm
              orderId={order.id}
              status={order.status}
              internalNote={order.internalNote}
            />
          </CardContent>
        </Card>
      </div>
    </AppShell>
  )
}
