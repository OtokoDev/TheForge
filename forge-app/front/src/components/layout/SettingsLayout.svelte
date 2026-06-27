<script>
  // Mise en page « config » à la Nexis : titre + sous-menu vertical à gauche, contenu à droite.
  let { title, subtitle = undefined, tabs = [], active, onSelect, children } = $props()
</script>

<div>
  <h1 class="text-2xl font-bold tracking-tight">{title}</h1>
  {#if subtitle}<p class="mt-1 text-sm text-muted-foreground">{subtitle}</p>{/if}
  <div class="mt-5 flex flex-col gap-6 md:flex-row">
    <nav class="flex shrink-0 flex-row flex-wrap gap-1 md:w-56 md:flex-col">
      {#each tabs as t}
        <button
          onclick={() => onSelect(t.key)}
          class="rounded-md px-3 py-2 text-left text-sm transition {active === t.key
            ? 'bg-sidebar-accent font-medium text-sidebar-accent-foreground'
            : 'text-muted-foreground hover:bg-muted hover:text-foreground'}"
        >
          {t.label}
        </button>
      {/each}
    </nav>
    <div class="min-w-0 flex-1">{@render children?.()}</div>
  </div>
</div>
