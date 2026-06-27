<script>
  let { variant = 'default', size = 'default', onclick, disabled = false, type = 'button', children } = $props()
  const variants = {
    default: 'bg-primary text-primary-foreground hover:bg-primary/90',
    outline: 'border bg-transparent hover:bg-accent hover:text-accent-foreground',
    destructive: 'bg-destructive text-white hover:bg-destructive/90',
  }
  const sizes = { default: 'h-10 px-4 py-2', sm: 'h-9 px-3', lg: 'h-11 px-6' }
</script>

<button
  {type}
  {disabled}
  {onclick}
  class="inline-flex items-center justify-center gap-2 rounded-md text-sm font-medium transition disabled:pointer-events-none disabled:opacity-50 {variants[variant] ?? variants.default} {sizes[size] ?? sizes.default}"
>
  {@render children?.()}
</button>
