import { Navbar } from "@/components/layout/navbar"
import { Sidebar } from "@/components/layout/sidebar"
import type { Role } from "@/lib/permissions"

export function AppShell({
  user,
  children,
}: {
  user: {
    name?: string | null
    inGameName?: string | null
    discordUsername?: string | null
    image?: string | null
    role: Role
  }
  children: React.ReactNode
}) {
  return (
    <div className="min-h-screen bg-background">
      <div className="flex">
        <Sidebar role={user.role} />
        <div className="min-w-0 flex-1">
          <Navbar user={user} />
          <main className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-6 lg:px-8">
            {children}
          </main>
        </div>
      </div>
    </div>
  )
}
