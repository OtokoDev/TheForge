<script>
  import { Minus, Plus } from '@lucide/svelte'

  // Contrôlé : le parent possède `value` (string), reçoit `onchange(newValue)`.
  // Steppers : clic = ±1, Ctrl = ±0.1, Shift = ±10, Ctrl+Shift = ±100.
  // variant="dark" : style inline assorti aux écrans bespoke (facturation/stock/catalogue/rachat).
  let { value = '', min = null, max = null, placeholder = '', disabled = false, class: cls = '', variant = 'default', onchange, onblur } = $props()

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
  const titleMinus = '−1 · Ctrl −0.1 · Shift −10 · Ctrl+Shift −100'
  const titlePlus = '+1 · Ctrl +0.1 · Shift +10 · Ctrl+Shift +100'
  let dark = $derived(variant === 'dark')
</script>

{#if dark}
  <div class={cls} style="display:inline-flex;align-items:stretch;overflow:hidden;border:1px solid rgba(255,255,255,0.12);border-radius:7px;background:#15110e;">
    <button type="button" {disabled} onclick={(e) => bump(e, -1)} title={titleMinus} style="width:26px;flex:none;background:#232120;border:none;border-right:1px solid rgba(255,255,255,0.1);color:#cfc8c2;cursor:pointer;display:flex;align-items:center;justify-content:center;"><Minus size={13} /></button>
    <input type="number" {value} {placeholder} {disabled} oninput={(e) => onchange?.(e.currentTarget.value)} {onblur} style="width:100%;min-width:0;background:transparent;border:none;color:#F4F1EE;font-size:13px;text-align:right;padding:5px 8px;outline:none;" />
    <button type="button" {disabled} onclick={(e) => bump(e, 1)} title={titlePlus} style="width:26px;flex:none;background:#232120;border:none;border-left:1px solid rgba(255,255,255,0.1);color:#cfc8c2;cursor:pointer;display:flex;align-items:center;justify-content:center;"><Plus size={13} /></button>
  </div>
{:else}
  <div class="inline-flex items-stretch overflow-hidden rounded-lg border border-input bg-input/30 {cls}">
    <button type="button" {disabled} onclick={(e) => bump(e, -1)} title={titleMinus} class="flex w-8 shrink-0 items-center justify-center border-r border-input text-muted-foreground transition hover:bg-muted hover:text-foreground disabled:opacity-50"><Minus size={14} /></button>
    <input type="number" {value} {placeholder} {disabled} oninput={(e) => onchange?.(e.currentTarget.value)} {onblur} class="w-full min-w-0 bg-transparent px-2 py-1 text-center text-sm outline-none" />
    <button type="button" {disabled} onclick={(e) => bump(e, 1)} title={titlePlus} class="flex w-8 shrink-0 items-center justify-center border-l border-input text-muted-foreground transition hover:bg-muted hover:text-foreground disabled:opacity-50"><Plus size={14} /></button>
  </div>
{/if}
