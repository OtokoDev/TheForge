import Link from "next/link"
import { Activity, Clock, Megaphone, Radio, Trophy, Users } from "lucide-react"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

export default async function AdminPage() {
  const session = await requireRole("ADMIN")
  const [users, logs, webhooks, smiths, shifts, announcements] = await Promise.all([
    prisma.user.count(),
    prisma.activityLog.count(),
    prisma.webhookLog.count(),
    prisma.user.count({ where: { role: { in: ["FORGERON", "GERANT", "ADMIN"] } } }),
    prisma.shopSession.count(),
    prisma.announcement.count(),
  ])

  return (
    <AppShell user={session.user}>
      <PageHeader title="Administration" description="Gestion des accès et pilotage de la forge." />
      <div className="grid gap-4 md:grid-cols-3 xl:grid-cols-6">
        <AdminCard icon={Users} title="Utilisateurs" value={users} href="/admin/utilisateurs" />
        <AdminCard icon={Trophy} title="Forgerons" value={smiths} href="/admin/forgerons" />
        <AdminCard icon={Clock} title="Prises de poste" value={shifts} href="/admin/prises-poste" />
        <AdminCard icon={Megaphone} title="Annonces" value={announcements} href="/admin/annonces" />
        <AdminCard icon={Activity} title="Logs activité" value={logs} href="/admin/logs" />
        <AdminCard icon={Radio} title="Logs webhooks" value={webhooks} href="/admin/webhooks" />
      </div>
    </AppShell>
  )
}

function AdminCard({
  icon: Icon,
  title,
  value,
  href,
}: {
  icon: typeof Users
  title: string
  value: number
  href: string
}) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>{title}</CardTitle>
          <Icon data-icon="inline-start" />
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-3xl font-semibold">{value}</p>
        <Link
          className={buttonVariants({ variant: "outline", size: "sm", className: "mt-4" })}
          href={href}
        >
          Ouvrir
        </Link>
      </CardContent>
    </Card>
  )
}
