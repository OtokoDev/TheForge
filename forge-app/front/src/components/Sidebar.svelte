<script>
  import { router } from 'svelte-spa-router'
  import {
    Gauge, FileText, ClipboardList, Hammer, Coins, ScrollText, ChartLine,
    Package, Settings, Globe, Boxes, Shield, User,
  } from '@lucide/svelte'
  import { me, currentBusinessId } from '../lib/session.js'
  import { canAdminBusiness, canStaffView } from '../lib/roles.js'

  const primaryBase = [
    { href: '/dashboard', label: 'Dashboard', icon: Gauge },
    { href: '/facturation', label: 'Facturation', icon: FileText },
    { href: '/commandes', label: 'Commandes', icon: ClipboardList },
    { href: '/stock', label: 'Stock', icon: Hammer },
    { href: '/rachat', label: 'Créances', icon: Coins },
    { href: '/main-courante', label: 'Main courante', icon: ScrollText },
    { href: '/statistiques', label: 'Statistiques', icon: ChartLine },
    { href: '/catalogue', label: 'Catalogue', icon: Package },
  ]

  let isSystem = $derived($me.user.globalRole === 'SYSTEM')
  let canConfig = $derived($currentBusinessId ? canAdminBusiness($me, $currentBusinessId) : false)

  let primary = $derived([
    ...primaryBase,
    ...(canConfig ? [{ href: '/configuration', label: 'Configuration', icon: Settings }] : []),
  ])
  let secondary = $derived([
    ...(canStaffView($me) ? [{ href: '/staff', label: 'Vue staff', icon: Globe }] : []),
    ...(isSystem ? [{ href: '/systeme', label: 'Système', icon: Boxes }] : []),
    ...(isSystem ? [{ href: '/admin', label: 'Administration', icon: Shield }] : []),
    { href: '/profil', label: 'Profil', icon: User },
  ])

  function isActive(href) {
    return router.location === href || router.location.startsWith(href + '/')
  }
</script>

<aside class="sticky top-0 hidden h-screen w-72 shrink-0 overflow-y-auto border-r bg-sidebar/80 px-4 py-5 lg:block">
  <div class="flex min-h-full flex-col">
    <a href="#/dashboard" class="flex items-center gap-3 rounded-md px-2 py-1.5">
      <div class="flex size-10 items-center justify-center rounded-md bg-primary text-primary-foreground">
        <Hammer size={20} />
      </div>
      <div class="min-w-0">
        <p class="truncate text-lg font-semibold leading-none">Forge RP</p>
        <p class="truncate text-xs text-muted-foreground">Gestion atelier</p>
      </div>
    </a>

    <nav class="mt-8 flex flex-col gap-1">
      {#each primary as item}
        {@const Icon = item.icon}
        <a
          href={'#' + item.href}
          aria-current={isActive(item.href) ? 'page' : undefined}
          class="flex h-10 items-center gap-3 rounded-md px-3 text-sm font-medium transition {isActive(item.href)
            ? 'bg-sidebar-accent text-sidebar-accent-foreground shadow-sm'
            : 'text-sidebar-foreground/75 hover:bg-sidebar-accent/70 hover:text-sidebar-accent-foreground'}"
        >
          <Icon size={18} />
          <span class="truncate">{item.label}</span>
        </a>
      {/each}
    </nav>

    <nav class="mt-auto flex flex-col gap-1 border-t pt-4">
      {#each secondary as item}
        {@const Icon = item.icon}
        <a
          href={'#' + item.href}
          aria-current={isActive(item.href) ? 'page' : undefined}
          class="flex h-10 items-center gap-3 rounded-md px-3 text-sm font-medium transition {isActive(item.href)
            ? 'bg-sidebar-accent text-sidebar-accent-foreground shadow-sm'
            : 'text-sidebar-foreground/75 hover:bg-sidebar-accent/70 hover:text-sidebar-accent-foreground'}"
        >
          <Icon size={18} />
          <span class="truncate">{item.label}</span>
        </a>
      {/each}
    </nav>
  </div>
</aside>
