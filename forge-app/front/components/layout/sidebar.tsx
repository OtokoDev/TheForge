"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import {
  Boxes,
  ChartLine,
  ClipboardList,
  Coins,
  FileText,
  Gauge,
  Globe,
  Hammer,
  Package,
  ScrollText,
  Settings,
  Shield,
  User,
  type LucideIcon,
} from "lucide-react"
import { cn } from "@/lib/utils"
import { useCurrentBusiness } from "@/lib/current-business"
import { canAdminBusiness, canStaffView } from "@/lib/roles"
import { useSession } from "@/lib/session"

type NavItem = {
  href: string
  label: string
  icon: LucideIcon
}

const primaryNavItems: NavItem[] = [
  { href: "/dashboard", label: "Dashboard", icon: Gauge },
  { href: "/facturation", label: "Facturation", icon: FileText },
  { href: "/commandes", label: "Commandes", icon: ClipboardList },
  { href: "/stock", label: "Stock", icon: Hammer },
  { href: "/rachat", label: "Créances", icon: Coins },
  { href: "/main-courante", label: "Main courante", icon: ScrollText },
  { href: "/statistiques", label: "Statistiques", icon: ChartLine },
  { href: "/catalogue", label: "Catalogue", icon: Package },
]

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

export function Sidebar() {
  const pathname = usePathname()
  const me = useSession()
  const { currentId } = useCurrentBusiness()

  const isSystem = me.user.globalRole === "SYSTEM"
  const canConfig = currentId ? canAdminBusiness(me, currentId) : false

  // Configuration = menu du business → dans le menu principal (pas dans les options du bas).
  const primary: NavItem[] = [
    ...primaryNavItems,
    ...(canConfig ? [{ href: "/configuration", label: "Configuration", icon: Settings }] : []),
  ]

  const secondaryNavItems: NavItem[] = [
    ...(canStaffView(me) ? [{ href: "/staff", label: "Vue staff", icon: Globe }] : []),
    ...(isSystem ? [{ href: "/systeme", label: "Système", icon: Boxes }] : []),
    ...(isSystem ? [{ href: "/admin", label: "Administration", icon: Shield }] : []),
    { href: "/profil", label: "Profil", icon: User },
  ]

  return (
    <aside className="sticky top-0 hidden h-screen w-72 shrink-0 overflow-y-auto border-r bg-sidebar/80 px-4 py-5 lg:block">
      <div className="flex min-h-full flex-col">
        <Link href="/dashboard" className="flex items-center gap-3 rounded-md px-2 py-1.5">
          <div className="flex size-10 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <Hammer data-icon="inline-start" />
          </div>
          <div className="min-w-0">
            <p className="truncate text-lg font-semibold leading-none">Forge RP</p>
            <p className="truncate text-xs text-muted-foreground">Gestion atelier</p>
          </div>
        </Link>

        <nav className="mt-8 flex flex-col gap-1">
          {primary.map((item) => (
            <NavLink key={item.href} item={item} pathname={pathname} />
          ))}
        </nav>

        <nav className="mt-auto flex flex-col gap-1 border-t pt-4">
          {secondaryNavItems.map((item) => (
            <NavLink key={item.href} item={item} pathname={pathname} />
          ))}
        </nav>
      </div>
    </aside>
  )
}
