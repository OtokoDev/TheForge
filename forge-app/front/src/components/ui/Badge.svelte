<script>
  let { variant = 'default', children } = $props()
  const styles = {
    default: 'border-transparent bg-primary text-primary-foreground',
    outline: 'text-foreground',
    secondary: 'border-transparent bg-secondary text-secondary-foreground',
  }
</script>

<span class="inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium {styles[variant] ?? styles.default}">
  {@render children?.()}
</span>
