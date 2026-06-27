<script>
  let { class: cls = '', children } = $props()
</script>

<div class="flex flex-col gap-4 overflow-hidden rounded-xl bg-card py-4 text-sm text-card-foreground ring-1 ring-foreground/10 {cls}">
  {@render children?.()}
</div>
