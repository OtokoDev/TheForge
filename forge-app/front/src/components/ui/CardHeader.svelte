<script>
  let { class: cls = '', children } = $props()
</script>

<div class="grid auto-rows-min items-start gap-1 px-4 {cls}">
  {@render children?.()}
</div>
