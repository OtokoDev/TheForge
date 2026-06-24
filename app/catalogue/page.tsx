import { CategoryManager } from "@/components/catalogue/category-manager"
import { ItemIcon } from "@/components/catalogue/item-icon"
import { ProductPriceEditor } from "@/components/catalogue/product-price-editor"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { recipeCost } from "@/lib/format"
import { hasRole, requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

export default async function CataloguePage() {
  const session = await requireRole("FORGERON")
  const products = await prisma.product.findMany({
    where: { isActive: true },
    include: { categoryRef: true, recipe: { include: { ingredient: true } } },
    orderBy: [{ category: "asc" }, { name: "asc" }],
  })
  const canEditPrices = hasRole(session.user.role, "ADMIN")

  const groupedProducts = products.reduce<Record<string, typeof products>>((groups, product) => {
    const category = product.categoryRef?.name ?? product.category
    groups[category] ??= []
    groups[category].push(product)
    return groups
  }, {})

  return (
    <AppShell user={session.user}>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <PageHeader title="Catalogue" description="Articles classés par catégorie." />
        {hasRole(session.user.role, "FORGERON") ? <CategoryManager /> : null}
      </div>

      {Object.entries(groupedProducts).map(([category, categoryProducts]) => (
        <section key={category} className="flex flex-col gap-3">
          <h3 className="text-lg font-semibold">{category}</h3>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {categoryProducts.map((product) => {
              const cost = recipeCost(product.recipe)
              return (
                <Card key={product.id}>
                  <CardHeader>
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <CardTitle>{product.name}</CardTitle>
                        <p className="mt-1 text-sm text-muted-foreground">
                          Stock fini : {product.finishedStock}
                        </p>
                      </div>
                      <ItemIcon itemType={product.itemType} handRequired={product.handRequired} />
                    </div>
                  </CardHeader>
                  <CardContent className="flex flex-col gap-4">
                    <p className="min-h-12 text-sm leading-6 text-muted-foreground">
                      {product.description ?? "Produit de forge prêt à être facturé."}
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {product.recipe.map((recipe) => (
                        <Badge key={recipe.id} variant="outline">
                          {recipe.quantity}x {recipe.ingredient.name}
                        </Badge>
                      ))}
                    </div>
                    <ProductPriceEditor
                      productId={product.id}
                      productName={product.name}
                      initialSellPrice={product.sellPrice}
                      unitCost={cost}
                      canEdit={canEditPrices}
                    />
                  </CardContent>
                </Card>
              )
            })}
          </div>
        </section>
      ))}
    </AppShell>
  )
}
