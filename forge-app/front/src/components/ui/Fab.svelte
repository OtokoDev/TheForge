<script>
  import { Plus } from '@lucide/svelte'

  // Bouton d'action flottant (mobile/tablette uniquement). L'action primaire des
  // écrans la garde en haut → enterrée au scroll sur petit écran. Le FAB la rend
  // toujours atteignable. Masqué en desktop (lg) où le bouton du haut suffit.
  let { onclick = () => {}, label = 'Ajouter', icon = Plus, badge = null } = $props()
  let Icon = $derived(icon)
</script>

<button
  {onclick}
  aria-label={label}
  class="fixed bottom-5 right-5 z-40 flex size-14 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-lg shadow-primary/40 transition active:scale-95 lg:hidden"
>
  <Icon size={26} />
  {#if badge != null && badge > 0}
    <span class="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-background px-1 text-xs font-bold text-foreground ring-2 ring-primary">{badge}</span>
  {/if}
</button>
