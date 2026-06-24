import { BarChartCard, LineChartCard } from "@/components/stats/charts"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { formatMoney } from "@/lib/format"
import { hasRole, requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"
import { sumByDay, topConsumedResources, topSoldItems } from "@/lib/stats"

type SearchParams = {
  period?: string
  forgeron?: string
  category?: string
  resource?: string
}

export default async function StatisticsPage({ searchParams }: { searchParams: SearchParams }) {
  const session = await requireRole("FORGERON")
  const isAdmin = hasRole(session.user.role, "ADMIN")
  const period = searchParams.period ?? "month"
  const startDate = getPeriodStart(period)
  const selectedForgeron = isAdmin ? searchParams.forgeron : session.user.id
  const selectedCategory = searchParams.category ?? ""
  const selectedResource = searchParams.resource ?? ""

  const [orders, users, categories, resources] = await Promise.all([
    prisma.order.findMany({
      where: {
        status: "LIVREE",
        createdAt: { gte: startDate },
        userId: selectedForgeron || undefined,
      },
      include: {
        user: true,
        items: { include: { product: true } },
        consumptions: { include: { ingredient: true } },
      },
      orderBy: { createdAt: "asc" },
    }),
    prisma.user.findMany({
      where: { role: { in: ["FORGERON", "GERANT", "ADMIN"] } },
      orderBy: [{ inGameName: "asc" }, { username: "asc" }],
    }),
    prisma.category.findMany({ orderBy: { name: "asc" } }),
    prisma.ingredient.findMany({ where: { isActive: true }, orderBy: { name: "asc" } }),
  ])

  const filteredOrders = selectedCategory
    ? orders.filter((order) =>
        order.items.some((item) => (item.product.category ?? "") === selectedCategory),
      )
    : orders
  const consumptions = filteredOrders
    .flatMap((order) => order.consumptions)
    .filter((consumption) => !selectedResource || consumption.ingredientId === selectedResource)
  const totalSales = filteredOrders.reduce((sum, order) => sum + order.totalPrice, 0)
  const totalCost = filteredOrders.reduce((sum, order) => sum + order.totalCost, 0)
  const totalProfit = filteredOrders.reduce((sum, order) => sum + order.totalProfit, 0)
  const companyShare = filteredOrders.reduce((sum, order) => sum + order.companyShare, 0)
  const smithShare = filteredOrders.reduce((sum, order) => sum + order.smithShare, 0)

  return (
    <AppShell user={session.user}>
      <PageHeader title="Statistiques" description="Ventes, bénéfices et consommation de ressources." />

      <Card>
        <CardHeader>
          <CardTitle>Filtres</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="grid gap-3 md:grid-cols-4">
            <select name="period" defaultValue={period} className="h-8 rounded-lg border bg-background px-2 text-sm">
              <option value="day">Jour</option>
              <option value="week">Semaine</option>
              <option value="month">Mois</option>
              <option value="year">Année</option>
            </select>
            {isAdmin ? (
              <select
                name="forgeron"
                defaultValue={selectedForgeron ?? ""}
                className="h-8 rounded-lg border bg-background px-2 text-sm"
              >
                <option value="">Tous les forgerons</option>
                {users.map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.inGameName ?? user.username}
                  </option>
                ))}
              </select>
            ) : null}
            <select name="category" defaultValue={selectedCategory} className="h-8 rounded-lg border bg-background px-2 text-sm">
              <option value="">Toutes catégories</option>
              {categories.map((category) => (
                <option key={category.id} value={category.name}>
                  {category.name}
                </option>
              ))}
            </select>
            <select name="resource" defaultValue={selectedResource} className="h-8 rounded-lg border bg-background px-2 text-sm">
              <option value="">Toutes ressources</option>
              {resources.map((resource) => (
                <option key={resource.id} value={resource.id}>
                  {resource.name}
                </option>
              ))}
            </select>
            <button className="h-8 rounded-lg bg-primary px-3 text-sm font-medium text-primary-foreground">
              Filtrer
            </button>
          </form>
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-6">
        <Metric title="Ventes" value={filteredOrders.length} />
        <Metric title="CA" value={formatMoney(totalSales)} />
        <Metric title="Prix de craft" value={formatMoney(totalCost)} />
        <Metric title="Bénéfice total" value={formatMoney(totalProfit)} />
        <Metric title="Part forge" value={formatMoney(companyShare)} />
        <Metric title="Part forgeron" value={formatMoney(smithShare)} />
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <LineChartCard title="Ventes dans le temps" points={sumByDay(filteredOrders, () => 1)} />
        <LineChartCard title="Chiffre d'affaires" points={sumByDay(filteredOrders, (order) => order.totalPrice)} money />
        <LineChartCard title="Bénéfice total" points={sumByDay(filteredOrders, (order) => order.totalProfit)} money />
        <LineChartCard title="Part forge" points={sumByDay(filteredOrders, (order) => order.companyShare)} money />
        <LineChartCard title="Part forgeron" points={sumByDay(filteredOrders, (order) => order.smithShare)} money />
        <BarChartCard title="Items les plus vendus" points={topSoldItems(filteredOrders)} />
        <BarChartCard title="Ressources consommées" points={topConsumedResources(consumptions)} />
        <BarChartCard
          title="Gains forgerons"
          points={forgeronPerformance(filteredOrders)}
          money
        />
      </div>
    </AppShell>
  )
}

function getPeriodStart(period: string) {
  const date = new Date()
  if (period === "day") date.setDate(date.getDate() - 1)
  else if (period === "week") date.setDate(date.getDate() - 7)
  else if (period === "year") date.setFullYear(date.getFullYear() - 1)
  else date.setMonth(date.getMonth() - 1)
  return date
}

function forgeronPerformance(orders: Awaited<ReturnType<typeof prisma.order.findMany>>) {
  const grouped = new Map<string, number>()
  for (const order of orders as any[]) {
    const label = order.user.inGameName ?? order.user.username
    grouped.set(label, (grouped.get(label) ?? 0) + order.smithShare)
  }

  return Array.from(grouped.entries())
    .map(([label, value]) => ({ label, value }))
    .sort((left, right) => right.value - left.value)
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
