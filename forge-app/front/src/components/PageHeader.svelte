<script>
  let { title, description } = $props()
</script>

<div class="flex flex-col gap-1">
  <h1 class="text-2xl font-semibold">{title}</h1>
  {#if description}
    <p class="text-sm text-muted-foreground">{description}</p>
  {/if}
</div>
