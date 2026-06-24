import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { StockInventoryTables } from "@/components/stock/stock-inventory-tables"
import { StockPanel } from "@/components/stock/stock-panel"
import { hasRole, requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

export default async function StockPage() {
  const session = await requireRole("FORGERON")
  const canManageStock = hasRole(session.user.role, "ADMIN")
  const canCreateIngredients = session.user.role === "ADMIN"
  const canEditIngredientSettings = hasRole(session.user.role, "GERANT")
  const [ingredients, products] = await Promise.all([
    prisma.ingredient.findMany({
      where: { isActive: true },
      orderBy: { name: "asc" },
      select: {
        id: true,
        name: true,
        currentStock: true,
        alertThreshold: true,
        unitCost: true,
      },
    }),
    prisma.product.findMany({
      where: { isActive: true },
      orderBy: { name: "asc" },
      select: {
        id: true,
        name: true,
        category: true,
        finishedStock: true,
        sellPrice: true,
      },
    }),
  ])

  return (
    <AppShell user={session.user}>
      <PageHeader title="Stock" description="Matières premières, produits finis et mouvements." />
      <StockPanel
        ingredients={ingredients}
        products={products}
        canCreateIngredients={canCreateIngredients}
      />
      <StockInventoryTables
        ingredients={ingredients}
        products={products}
        canDelete={canManageStock}
        canEditIngredientSettings={canEditIngredientSettings}
      />
    </AppShell>
  )
}
