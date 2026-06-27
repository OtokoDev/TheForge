<script>
  let {
    value = $bindable(''),
    placeholder = '',
    type = 'text',
    disabled = false,
    class: cls = '',
    oninput,
    onkeydown,
  } = $props()
</script>

<input
  {type}
  {placeholder}
  {disabled}
  {oninput}
  {onkeydown}
  bind:value
  class="h-8 w-full min-w-0 rounded-lg border border-input bg-input/30 px-2.5 py-1 text-sm outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50 {cls}"
/>
