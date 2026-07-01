<script>
  // Dropdown custom (popover) — remplace le <select> natif pour un rendu identique
  // et stylable partout (le menu natif était rendu blanc par l'OS).
  import { ChevronDown, Check } from '@lucide/svelte'

  let {
    value = '',
    options = [],
    onChange,
    placeholder = 'Choisir…',
    ariaLabel = undefined,
    disabled = false,
    class: cls = '',
  } = $props()

  let open = $state(false)
  let root = $state(null)
  let selected = $derived(options.find((o) => o.value === value))

  function toggle() {
    if (!disabled) open = !open
  }
  function pick(v) {
    onChange?.(v)
    open = false
  }
  $effect(() => {
    if (!open) return
    const onDoc = (e) => {
      if (root && !root.contains(e.target)) open = false
    }
    const onKey = (e) => {
      if (e.key === 'Escape') open = false
    }
    document.addEventListener('mousedown', onDoc)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDoc)
      document.removeEventListener('keydown', onKey)
    }
  })
</script>

<div bind:this={root} class="relative inline-flex {cls}">
  <button
    type="button"
    {disabled}
    aria-label={ariaLabel}
    aria-haspopup="listbox"
    aria-expanded={open}
    onclick={toggle}
    class="flex h-8 w-full items-center justify-between gap-2 rounded-lg border border-input bg-input/30 px-2.5 text-sm outline-none transition-colors hover:bg-input/50 focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
  >
    <span class="truncate {selected ? '' : 'text-muted-foreground'}">{selected?.label ?? placeholder}</span>
    <ChevronDown size={16} class="shrink-0 text-muted-foreground transition-transform {open ? 'rotate-180' : ''}" />
  </button>

  {#if open}
    <ul
      role="listbox"
      class="absolute left-0 top-full z-[60] mt-1 max-h-60 w-max min-w-full overflow-auto rounded-lg border bg-popover p-1 text-popover-foreground shadow-xl"
    >
      {#each options as o (o.value)}
        {@const active = o.value === value}
        <li>
          <button
            type="button"
            role="option"
            aria-selected={active}
            onclick={() => pick(o.value)}
            class="flex w-full items-center justify-between gap-3 whitespace-nowrap rounded-md px-2.5 py-1.5 text-left text-sm transition-colors {active
              ? 'bg-primary/15 text-primary'
              : 'hover:bg-muted'}"
          >
            <span>{o.label}</span>
            {#if active}<Check size={15} class="shrink-0" />{/if}
          </button>
        </li>
      {/each}
      {#if options.length === 0}
        <li class="px-2.5 py-1.5 text-sm text-muted-foreground">Aucune option</li>
      {/if}
    </ul>
  {/if}
</div>
