<script>
  import { X } from '@lucide/svelte'

  let { open = $bindable(false), title = '', children } = $props()
  const close = () => (open = false)
  function onKey(e) {
    if (e.key === 'Escape') close()
  }
</script>

<svelte:window onkeydown={onKey} />

{#if open}
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4">
    <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" onclick={close} role="presentation"></div>
    <div class="relative z-10 w-full max-w-lg rounded-xl border bg-card p-5 shadow-2xl">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-lg font-semibold">{title}</h2>
        <button onclick={close} class="text-muted-foreground transition hover:text-foreground" aria-label="Fermer"><X size={18} /></button>
      </div>
      {@render children?.()}
    </div>
  </div>
{/if}
