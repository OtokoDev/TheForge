<script>
  import { ChevronDown } from '@lucide/svelte'

  let { value = '', options = [], onChange, class: cls = '', ariaLabel = undefined, disabled = false } = $props()
</script>

<div class="relative inline-flex items-center {cls}">
  <select
    aria-label={ariaLabel}
    {disabled}
    {value}
    onchange={(e) => onChange?.(e.currentTarget.value)}
    style="color-scheme: dark"
    class="h-8 appearance-none rounded-lg border border-input bg-input/30 py-1 pr-8 pl-2.5 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
  >
    {#each options as o}
      <option value={o.value} class="bg-popover text-popover-foreground">{o.label}</option>
    {/each}
  </select>
  <ChevronDown size={16} class="pointer-events-none absolute right-2 text-muted-foreground" />
</div>
