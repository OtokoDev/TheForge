<script>
  import { ShieldAlert } from '@lucide/svelte'
</script>

<main class="flex min-h-[70vh] items-center justify-center px-6">
  <div class="max-w-md text-center">
    <div class="mx-auto flex size-12 items-center justify-center rounded-md bg-destructive/20 text-red-200">
      <ShieldAlert size={24} />
    </div>
    <h1 class="mt-5 text-3xl font-semibold">Accès refusé</h1>
    <p class="mt-3 text-sm leading-6 text-muted-foreground">
      Ton rôle Discord n'a pas encore les permissions nécessaires pour cette zone de la forge.
    </p>
    <a
      href="#/catalogue"
      class="mt-6 inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90"
    >
      Retour au catalogue
    </a>
  </div>
</main>
