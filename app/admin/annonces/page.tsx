import { AnnouncementForm } from "@/components/announcements/announcement-form"
import { AppShell } from "@/components/layout/app-shell"
import { PageHeader } from "@/components/layout/page-header"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { formatDateTime } from "@/lib/format"
import { requireRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"

const colorLabels = {
  GREEN: "Vert",
  YELLOW: "Jaune",
  RED: "Rouge",
}

const colorClasses = {
  GREEN: "border-emerald-500/40 bg-emerald-500/15 text-emerald-200",
  YELLOW: "border-yellow-500/40 bg-yellow-500/15 text-yellow-100",
  RED: "border-red-500/40 bg-red-500/15 text-red-200",
}

export default async function AdminAnnouncementsPage() {
  const session = await requireRole("ADMIN")
  const announcements = await prisma.announcement.findMany({
    include: { author: true },
    orderBy: { createdAt: "desc" },
    take: 20,
  })

  return (
    <AppShell user={session.user}>
      <PageHeader title="Annonces" description="Messages gérants affichés sur le dashboard." />
      <div className="grid gap-4 lg:grid-cols-[380px_1fr]">
        <Card>
          <CardHeader>
            <CardTitle>Nouvelle annonce</CardTitle>
          </CardHeader>
          <CardContent>
            <AnnouncementForm />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Historique des annonces</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {announcements.map((announcement) => (
              <div key={announcement.id} className="rounded-md border p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-medium">{announcement.title}</p>
                    <p className="mt-1 text-sm text-muted-foreground">{announcement.message}</p>
                    <p className="mt-2 text-xs text-muted-foreground">
                      {announcement.author.inGameName ?? announcement.author.username} ·{" "}
                      {formatDateTime(announcement.createdAt)}
                    </p>
                  </div>
                  <Badge variant="secondary" className={colorClasses[announcement.color]}>
                    {colorLabels[announcement.color]}
                  </Badge>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </AppShell>
  )
}
