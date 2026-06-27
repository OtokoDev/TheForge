import { BusinessConfig } from "@/components/business/business-config"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"

export default function ConfigurationPage() {
  return (
    <AppShell>
      <PageHeader title="Configuration" description="Paramètres du business courant (admin)." />
      <BusinessConfig />
    </AppShell>
  )
}
