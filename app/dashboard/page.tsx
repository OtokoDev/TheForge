import Link from "next/link"
import { AlertTriangle, Coins, Flame, Megaphone, PackageCheck } from "lucide-react"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { StatusBadge } from "@/components/orders/status-badge"
import { ShopToggle } from "@/components/shop/shop-toggle"
import { Badge } from "@/components/ui/badge"
import { buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatDateTime, formatMoney, formatOrderNumber } from "@/lib/format"
import { requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

const announcementColorClasses = {
  GREEN: "border-emerald-500/40 bg-emerald-500/15 text-emerald-100",
  YELLOW: "border-yellow-500/40 bg-yellow-500/15 text-yellow-100",
  RED: "border-red-500/40 bg-red-500/15 text-red-100",
}

export default async function DashboardPage() {
  const session = await requireRole("FORGERON")
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const [
    shopSession,
    pendingOrders,
    craftingOrders,
    ingredients,
    latestOrders,
    deliveredToday,
    announcements,
  ] = await Promise.all([
    prisma.shopSession.findFirst({
      where: { closedAt: null },
      include: { user: true },
    }),
    prisma.order.count({ where: { status: "EN_ATTENTE" } }),
    prisma.order.count({ where: { status: "EN_FABRICATION" } }),
    prisma.ingredient.findMany({
      where: { isActive: true },
      orderBy: { currentStock: "asc" },
    }),
    prisma.order.findMany({
      take: 5,
      include: { user: true },
      orderBy: { createdAt: "desc" },
    }),
    prisma.order.findMany({ where: { status: "LIVREE", deliveredAt: { gte: today } } }),
    prisma.announcement.findMany({
      where: { isActive: true },
      include: { author: true },
      orderBy: { createdAt: "desc" },
      take: 5,
    }),
  ])

  const lowStock = ingredients.filter((item) => item.currentStock <= item.alertThreshold).slice(0, 6)
  const todaySales = deliveredToday.reduce((total, order) => total + order.totalPrice, 0)
  const todayProfit = deliveredToday.reduce((total, order) => total + order.totalProfit, 0)
  const todayCompanyShare = deliveredToday.reduce((total, order) => total + order.companyShare, 0)
  const todaySmithShare = deliveredToday.reduce((total, order) => total + order.smithShare, 0)

  return (
    <AppShell user={session.user}>
      <PageHeader
        title="Dashboard"
        description="Prise de poste, commandes, annonces et alertes de la forge."
      />

      {announcements.length > 0 ? (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Megaphone data-icon="inline-start" />
              Annonces gérants
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3">
            {announcements.map((announcement) => (
              <div
                key={announcement.id}
                className={`rounded-md border p-4 ${announcementColorClasses[announcement.color]}`}
              >
                <p className="font-medium">{announcement.title}</p>
                <p className="mt-1 text-sm">{announcement.message}</p>
                <p className="mt-2 text-xs opacity-80">
                  {announcement.author.inGameName ?? announcement.author.username} ·{" "}
                  {formatDateTime(announcement.createdAt)}
                </p>
              </div>
            ))}
          </CardContent>
        </Card>
      ) : null}

      <Card>
        <CardContent className="flex flex-col gap-5 p-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <Badge
              variant="secondary"
              className={
                shopSession
                  ? "border-emerald-500/40 bg-emerald-500/15 text-emerald-100"
                  : "border-red-500/40 bg-red-500/15 text-red-100"
              }
            >
              {shopSession ? "Poste ouvert" : "Poste fermé"}
            </Badge>
            <p className="mt-3 text-sm text-muted-foreground">
              {shopSession
                ? `Pris par ${shopSession.user.inGameName ?? shopSession.user.username} à ${formatDateTime(shopSession.openedAt)}`
                : "Aucune prise de poste active."}
            </p>
          </div>
          <ShopToggle isOpen={Boolean(shopSession)} />
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={Flame}
          title="Poste"
          value={shopSession ? "Ouvert" : "Fermé"}
          hint={shopSession ? `Par ${shopSession.user.inGameName ?? shopSession.user.username}` : "Aucun poste actif"}
        />
        <MetricCard icon={PackageCheck} title="Commandes en attente" value={pendingOrders} />
        <MetricCard icon={PackageCheck} title="En fabrication" value={craftingOrders} />
        <MetricCard icon={Coins} title="CA du jour" value={formatMoney(todaySales)} />
        <MetricCard icon={Coins} title="Bénéfice total" value={formatMoney(todayProfit)} />
        <MetricCard icon={Coins} title="Part forge" value={formatMoney(todayCompanyShare)} />
        <MetricCard icon={Coins} title="Part forgeron" value={formatMoney(todaySmithShare)} />
      </div>

      <div className="grid gap-4 xl:grid-cols-[1fr_380px]">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Dernières commandes</CardTitle>
            <Link
              className={buttonVariants({ variant: "outline", size: "sm" })}
              href="/commandes"
            >
              Tout voir
            </Link>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>N°</TableHead>
                  <TableHead>Client</TableHead>
                  <TableHead>Statut</TableHead>
                  <TableHead>Total</TableHead>
                  <TableHead>Date</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {latestOrders.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell>{formatOrderNumber(order.orderNumber)}</TableCell>
                    <TableCell>{order.user.inGameName ?? order.user.username}</TableCell>
                    <TableCell>
                      <StatusBadge status={order.status} />
                    </TableCell>
                    <TableCell>{formatMoney(order.totalPrice)}</TableCell>
                    <TableCell>{formatDateTime(order.createdAt)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Stock bas</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {lowStock.length === 0 ? (
              <p className="text-sm text-muted-foreground">Aucune alerte de stock.</p>
            ) : (
              lowStock.map((item) => (
                <div key={item.id} className="flex items-center justify-between rounded-md border p-3">
                  <div>
                    <p className="font-medium">{item.name}</p>
                    <p className="text-xs text-muted-foreground">Seuil {item.alertThreshold}</p>
                  </div>
                  <Badge variant="secondary" className="border-yellow-500/40 bg-yellow-500/15">
                    <AlertTriangle data-icon="inline-start" />
                    {item.currentStock}
                  </Badge>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </div>
    </AppShell>
  )
}

function MetricCard({
  icon: Icon,
  title,
  value,
  hint,
}: {
  icon: typeof Flame
  title: string
  value: string | number
  hint?: string
}) {
  return (
    <Card>
      <CardContent className="flex items-center gap-4 p-5">
        <div className="flex size-11 items-center justify-center rounded-md bg-accent text-primary">
          <Icon data-icon="inline-start" />
        </div>
        <div>
          <p className="text-sm text-muted-foreground">{title}</p>
          <p className="text-2xl font-semibold">{value}</p>
          {hint ? <p className="text-xs text-muted-foreground">{hint}</p> : null}
        </div>
      </CardContent>
    </Card>
  )
}
