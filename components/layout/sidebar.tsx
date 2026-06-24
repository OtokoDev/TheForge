"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import {
  ChartLine,
  ClipboardList,
  Coins,
  FileText,
  Gauge,
  Hammer,
  Package,
  Settings,
  User,
  type LucideIcon,
} from "lucide-react"
import type { Role } from "@/lib/permissions"
import { cn } from "@/lib/utils"

type NavItem = {
  href: string
  label: string
  icon: LucideIcon
  minRole: Role
}

const primaryNavItems = [
  { href: "/dashboard", label: "Dashboard", icon: Gauge, minRole: "FORGERON" },
  { href: "/facturation", label: "Facturation", icon: FileText, minRole: "FORGERON" },
  { href: "/commandes", label: "Commandes", icon: ClipboardList, minRole: "FORGERON" },
  { href: "/stock", label: "Stock", icon: Hammer, minRole: "FORGERON" },
  { href: "/rachat", label: "Rachat", icon: Coins, minRole: "FORGERON" },
  { href: "/statistiques", label: "Statistiques", icon: ChartLine, minRole: "FORGERON" },
  { href: "/catalogue", label: "Catalogue", icon: Package, minRole: "FORGERON" },
] satisfies NavItem[]

const secondaryNavItems = [
  { href: "/admin", label: "Administration", icon: Settings, minRole: "ADMIN" },
  { href: "/profil", label: "Profil", icon: User, minRole: "FORGERON" },
] satisfies NavItem[]

const hierarchy: Role[] = ["FORGERON", "GERANT", "ADMIN"]

function canAccess(role: Role, minRole: Role) {
  return hierarchy.indexOf(role) >= hierarchy.indexOf(minRole)
}

function isCurrentPath(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(`${href}/`)
}

function NavLink({ item, pathname }: { item: NavItem; pathname: string }) {
  const Icon = item.icon
  const active = isCurrentPath(pathname, item.href)

  return (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "flex h-10 items-center gap-3 rounded-md px-3 text-sm font-medium transition",
        active
          ? "bg-sidebar-accent text-sidebar-accent-foreground shadow-sm"
          : "text-sidebar-foreground/75 hover:bg-sidebar-accent/70 hover:text-sidebar-accent-foreground",
      )}
    >
      <Icon data-icon="inline-start" />
      <span className="truncate">{item.label}</span>
    </Link>
  )
}

export function Sidebar({ role }: { role: Role }) {
  const pathname = usePathname()
  const homeHref = "/dashboard"
  const primaryItems = primaryNavItems.filter((item) => canAccess(role, item.minRole))
  const secondaryItems = secondaryNavItems.filter((item) => canAccess(role, item.minRole))

  return (
    <aside className="hidden min-h-screen w-72 shrink-0 border-r bg-sidebar/80 px-4 py-5 lg:block">
      <div className="flex h-[calc(100vh-2.5rem)] flex-col">
        <Link href={homeHref} className="flex items-center gap-3 rounded-md px-2 py-1.5">
          <div className="flex size-10 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <Hammer data-icon="inline-start" />
          </div>
          <div className="min-w-0">
            <p className="truncate text-lg font-semibold leading-none">Forge RP</p>
            <p className="truncate text-xs text-muted-foreground">Gestion atelier</p>
          </div>
        </Link>

        <nav className="mt-8 flex flex-col gap-1">
          {primaryItems.map((item) => (
            <NavLink key={item.href} item={item} pathname={pathname} />
          ))}
        </nav>

        <nav className="mt-auto flex flex-col gap-1 border-t pt-4">
          {secondaryItems.map((item) => (
            <NavLink key={item.href} item={item} pathname={pathname} />
          ))}
        </nav>
      </div>
    </aside>
  )
}
