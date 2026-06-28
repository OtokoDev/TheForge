<script>
  import { Minus, Plus } from '@lucide/svelte'

  // Contrôlé : le parent possède `value` (string), reçoit `onchange(newValue)`.
  // Steppers : clic = ±1, Ctrl = ±0.1, Shift = ±10, Ctrl+Shift = ±100.
  let { value = '', min = null, max = null, placeholder = '', disabled = false, class: cls = '', onchange, onblur } = $props()

  function amount(e) {
    if (e.ctrlKey && e.shiftKey) return 100
    if (e.shiftKey) return 10
    if (e.ctrlKey) return 0.1
    return 1
  }
  function clamp(v) {
    if (min != null && v < min) v = min
    if (max != null && v > max) v = max
    return v
  }
  function bump(e, dir) {
    const cur = Number(value) || 0
    const next = Math.round((cur + dir * amount(e)) * 1000) / 1000
    onchange?.(String(clamp(next)))
    onblur?.()
  }
</script>

<div class="inline-flex items-stretch overflow-hidden rounded-lg border border-input bg-input/30 {cls}">
  <button
    type="button"
    {disabled}
    onclick={(e) => bump(e, -1)}
    title="−1 · Ctrl −0.1 · Shift −10 · Ctrl+Shift −100"
    class="flex w-8 shrink-0 items-center justify-center border-r border-input text-muted-foreground transition hover:bg-muted hover:text-foreground disabled:opacity-50"
  >
    <Minus size={14} />
  </button>
  <input
    type="number"
    {value}
    {placeholder}
    {disabled}
    oninput={(e) => onchange?.(e.currentTarget.value)}
    {onblur}
    class="w-full min-w-0 bg-transparent px-2 py-1 text-center text-sm outline-none"
  />
  <button
    type="button"
    {disabled}
    onclick={(e) => bump(e, 1)}
    title="+1 · Ctrl +0.1 · Shift +10 · Ctrl+Shift +100"
    class="flex w-8 shrink-0 items-center justify-center border-l border-input text-muted-foreground transition hover:bg-muted hover:text-foreground disabled:opacity-50"
  >
    <Plus size={14} />
  </button>
</div>
