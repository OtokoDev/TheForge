<script>
  import { onMount } from 'svelte'
  import Router, { router, replace } from 'svelte-spa-router'
  import { api, ApiError, setUnauthorizedHandler } from './lib/api.js'
  import { me, currentBusiness, initBusiness } from './lib/session.js'
  import { ROUTE_ALIASES } from './lib/screens.js'
  import { startRealtime, onRealtime } from './lib/realtime.js'
  import { routes } from './lib/routes.js'
  import Sidebar from './components/Sidebar.svelte'
  import Navbar from './components/Navbar.svelte'
  import PseudoWarning from './components/PseudoWarning.svelte'
  import ToastHost from './components/ToastHost.svelte'
  import Login from './pages/Login.svelte'

  // Garde d'auth pilotée par le store `me`. Tout 401 (token expiré, n'importe quel appel)
  // remet `me` à null → l'écran de login s'affiche automatiquement (plus de balade en 401).
  let loaded = $state(false)
  let errored = $state(false)
  let mobileNav = $state(false)

  // Garde de visibilité : un écran masqué pour ce business (par SYSTEM) redirige vers le dashboard,
  // pour tout le monde (SYSTEM inclus). SYSTEM réactive via Système. Front-only (déclutter) :
  // n'empêche pas l'appel direct des API.
  $effect(() => {
    if (!$me) return
    const hidden = $currentBusiness?.hiddenScreens ?? []
    const loc = router.location
    if (!loc) return
    const eff = ROUTE_ALIASES[loc] ?? loc
    if (hidden.some((h) => eff === h || eff.startsWith(h + '/'))) replace('/dashboard')
  })

  onMount(async () => {
    setUnauthorizedHandler(() => me.set(null))
    try {
      const profile = await api('/api/me')
      me.set(profile)
      await initBusiness()
      startRealtime()
      // Bannissement poussé par le serveur (WS) → déconnexion immédiate de la session.
      onRealtime('REVOKED', () => me.set(null))
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 401)) errored = true
      // 401 → me reste null → Login
    } finally {
      loaded = true
    }
  })
</script>

{#if !loaded}
  <div class="flex min-h-screen items-center justify-center text-muted-foreground">Chargement…</div>
{:else if errored}
  <div class="flex min-h-screen items-center justify-center text-destructive">
    Erreur de chargement de la session.
  </div>
{:else if !$me}
  <Login />
{:else}
  <div class="min-h-screen bg-background">
    <div class="flex">
      <Sidebar open={mobileNav} onClose={() => (mobileNav = false)} />
      <div class="min-w-0 flex-1">
        <Navbar onMenu={() => (mobileNav = true)} />
        <PseudoWarning />
        <main class="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-6 lg:px-8">
          <Router {routes} />
        </main>
      </div>
    </div>
  </div>
{/if}

<ToastHost />
