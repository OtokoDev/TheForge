import { ProfileForm } from "@/components/profile/profile-form"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatDateTime, formatMoney } from "@/lib/format"
import { requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

export default async function ProfilePage() {
  const session = await requireRole("FORGERON")
  const [user, orders, sessions] = await Promise.all([
    prisma.user.findUniqueOrThrow({ where: { id: session.user.id } }),
    prisma.order.findMany({
      where: { userId: session.user.id, status: "LIVREE" },
      include: { items: { include: { product: true } } },
      orderBy: { createdAt: "desc" },
    }),
    prisma.shopSession.findMany({ where: { userId: session.user.id } }),
  ])

  const totalSales = orders.reduce((sum, order) => sum + order.totalPrice, 0)
  const totalProfit = orders.reduce((sum, order) => sum + order.totalProfit, 0)
  const totalCompanyShare = orders.reduce((sum, order) => sum + order.companyShare, 0)
  const totalSmithShare = orders.reduce((sum, order) => sum + order.smithShare, 0)
  const workedMinutes = sessions.reduce((sum, shopSession) => {
    const end = shopSession.closedAt ?? new Date()
    return sum + Math.max(0, new Date(end).getTime() - shopSession.openedAt.getTime()) / 60000
  }, 0)

  return (
    <AppShell user={session.user}>
      <PageHeader title="Profil" description="Pseudo IG, ventes et performance personnelle." />
      <div className="grid gap-4 lg:grid-cols-[360px_1fr]">
        <Card>
          <CardHeader>
            <CardTitle>Identité</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <ProfileForm initialName={user.inGameName ?? ""} />
            <div className="text-sm text-muted-foreground">
              <p>Discord : {user.username}</p>
              <p>ID : {user.discordId}</p>
            </div>
          </CardContent>
        </Card>

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-6">
          <Metric title="Ventes" value={orders.length} />
          <Metric title="CA total" value={formatMoney(totalSales)} />
          <Metric title="Bénéfice total" value={formatMoney(totalProfit)} />
          <Metric title="Part forge" value={formatMoney(totalCompanyShare)} />
          <Metric title="Part forgeron" value={formatMoney(totalSmithShare)} />
          <Metric title="Heures travaillées" value={formatWorkedMinutes(workedMinutes)} />
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Historique des ventes</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Date</TableHead>
                <TableHead>Items</TableHead>
                <TableHead>CA</TableHead>
                <TableHead>Bénéfice total</TableHead>
                <TableHead>Part forge</TableHead>
                <TableHead>Part forgeron</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {orders.map((order) => (
                <TableRow key={order.id}>
                  <TableCell>{formatDateTime(order.createdAt)}</TableCell>
                  <TableCell>
                    {order.items.map((item) => `${item.quantity}x ${item.product.name}`).join(", ")}
                  </TableCell>
                  <TableCell>{formatMoney(order.totalPrice)}</TableCell>
                  <TableCell>{formatMoney(order.totalProfit)}</TableCell>
                  <TableCell>{formatMoney(order.companyShare)}</TableCell>
                  <TableCell>{formatMoney(order.smithShare)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Historique de prise de poste</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Ouverture</TableHead>
                <TableHead>Fermeture</TableHead>
                <TableHead>Durée</TableHead>
                <TableHead>Factures</TableHead>
                <TableHead>CA</TableHead>
                <TableHead>Bénéfice total</TableHead>
                <TableHead>Part forge</TableHead>
                <TableHead>Part forgeron</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {sessions.map((shopSession) => (
                <TableRow key={shopSession.id}>
                  <TableCell>{formatDateTime(shopSession.openedAt)}</TableCell>
                  <TableCell>
                    {shopSession.closedAt ? formatDateTime(shopSession.closedAt) : "En cours"}
                  </TableCell>
                  <TableCell>
                    {formatWorkedMinutes(
                      Math.max(
                        0,
                        ((shopSession.closedAt ?? new Date()).getTime() -
                          shopSession.openedAt.getTime()) /
                          60000,
                      ),
                    )}
                  </TableCell>
                  <TableCell>{shopSession.ordersCount}</TableCell>
                  <TableCell>{formatMoney(shopSession.totalSales)}</TableCell>
                  <TableCell>{formatMoney(shopSession.totalProfit)}</TableCell>
                  <TableCell>{formatMoney(shopSession.totalProfit * 0.5)}</TableCell>
                  <TableCell>{formatMoney(shopSession.totalProfit * 0.5)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </AppShell>
  )
}

function Metric({ title, value }: { title: string; value: string | number }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm text-muted-foreground">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-2xl font-semibold">{value}</p>
      </CardContent>
    </Card>
  )
}

function formatWorkedMinutes(minutes: number) {
  const totalMinutes = Math.floor(minutes)
  const hours = Math.floor(totalMinutes / 60)
  const rest = totalMinutes % 60
  return hours > 0 ? `${hours}h ${rest}min` : `${rest}min`
}
