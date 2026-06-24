import { InvoicePanel } from "@/components/invoice/invoice-panel"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { recipeCost } from "@/lib/format"
import { requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

export default async function FacturationPage() {
  const session = await requireRole("FORGERON")
  const products = await prisma.product.findMany({
    where: { isActive: true },
    include: { categoryRef: true, recipe: { include: { ingredient: true } } },
    orderBy: [{ category: "asc" }, { name: "asc" }],
  })

  return (
    <AppShell user={session.user}>
      <PageHeader
        title="Facturation"
        description="Clique les articles vendus pour obtenir le prix, le coût et la marge."
      />
      <InvoicePanel
        products={products.map((product) => ({
          id: product.id,
          name: product.name,
          category: product.categoryRef?.name ?? product.category,
          sellPrice: product.sellPrice,
          unitCost: recipeCost(product.recipe),
          finishedStock: product.finishedStock,
          itemType: product.itemType,
          handRequired: product.handRequired,
        }))}
      />
    </AppShell>
  )
}
