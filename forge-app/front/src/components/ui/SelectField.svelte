<script>
  // Dropdown custom : trigger stylé + liste en popover. La liste est portée vers <body>
  // en position fixed pour ne jamais être clippée par un parent en overflow (modale, carte, table).
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
  let trigger = $state(null)
  let pos = $state({ left: 0, top: 0, width: 0 })
  let selected = $derived(options.find((o) => o.value === value))

  function place() {
    if (!trigger) return
    const r = trigger.getBoundingClientRect()
    pos = { left: r.left, top: r.bottom + 4, width: r.width }
  }
  function toggle() {
    if (disabled) return
    if (!open) place()
    open = !open
  }
  function pick(v) {
    onChange?.(v)
    open = false
  }

  // Déplace le nœud vers <body> (sort de tout conteneur qui clippe).
  function portal(node) {
    document.body.appendChild(node)
    return { destroy: () => node.remove() }
  }

  $effect(() => {
    if (!open) return
    const onDoc = (e) => {
      if (trigger?.contains(e.target)) return
      if (e.target.closest?.('[data-select-pop]')) return
      open = false
    }
    const close = () => (open = false)
    const onKey = (e) => {
      if (e.key === 'Escape') open = false
    }
    document.addEventListener('mousedown', onDoc)
    window.addEventListener('scroll', close, true)
    window.addEventListener('resize', close)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDoc)
      window.removeEventListener('scroll', close, true)
      window.removeEventListener('resize', close)
      document.removeEventListener('keydown', onKey)
    }
  })
</script>

<div class="relative inline-flex {cls}">
  <button
    bind:this={trigger}
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
</div>

{#if open}
  <ul
    use:portal
    data-select-pop
    role="listbox"
    style="position:fixed; left:{pos.left}px; top:{pos.top}px; min-width:{pos.width}px;"
    class="z-[1000] max-h-60 w-max overflow-auto rounded-lg border bg-popover p-1 text-popover-foreground shadow-xl"
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
