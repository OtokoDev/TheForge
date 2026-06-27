<script>
  import { Hammer, LogIn, ShieldCheck, Store } from '@lucide/svelte'
  import { LOGIN_URL } from '../lib/api.js'

  const features = [
    { icon: Store, title: 'Multi-business', text: 'Forge, compagnie et autres activités, cloisonnées par business.' },
    { icon: Hammer, title: 'Stock & comptes', text: 'Journal de mouvements traçable, stock et trésorerie unifiés.' },
    { icon: ShieldCheck, title: 'Rôles', text: 'Accès SYSTEM, STAFF et par business (ADMIN / MEMBRE) après connexion Discord.' },
  ]
</script>

<main class="min-h-screen bg-background">
  <section class="mx-auto flex min-h-screen w-full max-w-6xl flex-col justify-center gap-10 px-6 py-10">
    <div class="max-w-3xl">
      <div class="mb-6 flex size-14 items-center justify-center rounded-md bg-primary text-primary-foreground">
        <Hammer size={28} />
      </div>
      <h1 class="max-w-2xl text-5xl font-semibold tracking-normal text-foreground sm:text-6xl">Forge RP</h1>
      <p class="mt-5 max-w-2xl text-lg leading-8 text-muted-foreground">
        Gestion de stock, comptes et mouvements pour les activités économiques d'un serveur
        roleplay, connectée à Discord.
      </p>
      <div class="mt-8">
        <button
          class="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-primary px-6 text-sm font-medium text-primary-foreground transition hover:bg-primary/90"
          onclick={() => (window.location.href = LOGIN_URL)}
        >
          <LogIn size={18} />
          Se connecter avec Discord
        </button>
      </div>
    </div>

    <div class="grid gap-4 md:grid-cols-3">
      {#each features as item}
        {@const Icon = item.icon}
        <div class="rounded-lg border bg-card text-card-foreground shadow-sm">
          <div class="flex gap-4 p-5">
            <div class="flex size-10 shrink-0 items-center justify-center rounded-md bg-accent text-primary">
              <Icon size={20} />
            </div>
            <div>
              <h2 class="font-semibold">{item.title}</h2>
              <p class="mt-1 text-sm leading-6 text-muted-foreground">{item.text}</p>
            </div>
          </div>
        </div>
      {/each}
    </div>
  </section>
</main>
