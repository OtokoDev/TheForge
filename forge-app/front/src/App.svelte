<script>
  import { onMount } from 'svelte'
  import Router from 'svelte-spa-router'
  import { api, ApiError, setUnauthorizedHandler } from './lib/api.js'
  import { me, initBusiness } from './lib/session.js'
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
      <Sidebar />
      <div class="min-w-0 flex-1">
        <Navbar />
        <PseudoWarning />
        <main class="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-6 lg:px-8">
          <Router {routes} />
        </main>
      </div>
    </div>
  </div>
{/if}

<ToastHost />
