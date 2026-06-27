<script>
  let { class: cls = '', children } = $props()
</script>

<div class="px-4 {cls}">
  {@render children?.()}
</div>
