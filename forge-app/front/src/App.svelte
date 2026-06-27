<script>
  import { onMount } from 'svelte'
  import Router from 'svelte-spa-router'
  import { api, ApiError } from './lib/api.js'
  import { me, initBusiness } from './lib/session.js'
  import { startRealtime } from './lib/realtime.js'
  import { routes } from './lib/routes.js'
  import Sidebar from './components/Sidebar.svelte'
  import Navbar from './components/Navbar.svelte'
  import ToastHost from './components/ToastHost.svelte'
  import Login from './pages/Login.svelte'

  // Garde d'auth : charge /api/me. 401 → écran de connexion ; sinon shell + routeur.
  let state = $state('loading')

  onMount(async () => {
    try {
      const profile = await api('/api/me')
      me.set(profile)
      await initBusiness()
      startRealtime()
      state = 'ready'
    } catch (e) {
      state = e instanceof ApiError && e.status === 401 ? 'anon' : 'error'
    }
  })
</script>

{#if state === 'loading'}
  <div class="flex min-h-screen items-center justify-center text-muted-foreground">Chargement…</div>
{:else if state === 'anon'}
  <Login />
{:else if state === 'error'}
  <div class="flex min-h-screen items-center justify-center text-destructive">
    Erreur de chargement de la session.
  </div>
{:else}
  <div class="min-h-screen bg-background">
    <div class="flex">
      <Sidebar />
      <div class="min-w-0 flex-1">
        <Navbar />
        <main class="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-6 lg:px-8">
          <Router {routes} />
        </main>
      </div>
    </div>
  </div>
{/if}

<ToastHost />
