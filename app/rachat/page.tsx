import { BuybackMaterialsPanel } from "@/components/buyback/buyback-materials-panel"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { hasRole, requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

export default async function RachatPage() {
  const session = await requireRole("FORGERON")
  const materials = await prisma.ingredient.findMany({
    where: { materialType: "RAW", isActive: true },
    orderBy: { name: "asc" },
  })

  return (
    <AppShell user={session.user}>
      <PageHeader
        title="Rachat"
        description="Prix de rachat des matières premières. Les admins peuvent les modifier."
      />
      <BuybackMaterialsPanel materials={materials} canEdit={hasRole(session.user.role, "ADMIN")} />
    </AppShell>
  )
}
