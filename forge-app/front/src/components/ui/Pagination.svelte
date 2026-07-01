<script>
  import SelectField from './SelectField.svelte'
  // Pagination client : le parent découpe rows.slice((page-1)*pageSize, page*pageSize).
  let { page = $bindable(1), pageSize = $bindable(50), total = 0, sizes = [50, 100, 200] } = $props()
  let pages = $derived(Math.max(1, Math.ceil(total / pageSize)))
  $effect(() => {
    if (page > pages) page = pages
  })
  const go = (p) => (page = Math.min(pages, Math.max(1, p)))
</script>

<div class="flex flex-wrap items-center gap-3 py-2 text-sm">
  <span class="text-muted-foreground">{total} résultat{total > 1 ? 's' : ''}</span>
  <label class="flex items-center gap-1 text-muted-foreground">
    Par page
    <SelectField value={pageSize} onChange={(v) => (pageSize = v)} options={sizes.map((s) => ({ value: s, label: String(s) }))} ariaLabel="Résultats par page" />
  </label>
  <div class="ml-auto flex items-center gap-2">
    <button disabled={page <= 1} onclick={() => go(page - 1)} class="rounded-md border px-2.5 py-1 disabled:opacity-40" aria-label="Page précédente">‹</button>
    <span>{page} / {pages}</span>
    <button disabled={page >= pages} onclick={() => go(page + 1)} class="rounded-md border px-2.5 py-1 disabled:opacity-40" aria-label="Page suivante">›</button>
  </div>
</div>
