<script>
  import { Check } from '@lucide/svelte'

  let { checked = $bindable(false), onchange, label = '', disabled = false, class: cls = '' } = $props()
</script>

<label class="inline-flex cursor-pointer items-center gap-2 text-sm {disabled ? 'opacity-50' : ''} {cls}">
  <input type="checkbox" bind:checked {onchange} {disabled} class="sr-only" />
  <span
    class="flex size-4 shrink-0 items-center justify-center rounded border transition {checked
      ? 'border-primary bg-primary text-primary-foreground'
      : 'border-input bg-input/30'}"
  >
    {#if checked}<Check size={12} stroke-width={3} />{/if}
  </span>
  {#if label}<span>{label}</span>{/if}
</label>
