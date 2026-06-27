<script>
  let { class: cls = '', children } = $props()
</script>

<div class="text-base leading-snug font-medium {cls}">
  {@render children?.()}
</div>
