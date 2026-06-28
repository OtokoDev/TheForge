<script>
  import { Trash2 } from '@lucide/svelte'
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Button from '../ui/Button.svelte'
  import SelectField from '../ui/SelectField.svelte'
  import NumberInput from '../ui/NumberInput.svelte'

  let { output, items } = $props()

  let lines = $state(null)
  let candidates = $derived(items.filter((i) => i.id !== output.id))
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  $effect(() => {
    let active = true
    api(`/api/catalog/items/${output.id}/recipe`)
      .then((rows) => active && (lines = rows.map((r) => ({ componentItemId: r.componentItemId, quantity: r.quantity }))))
      .catch(fail)
    return () => { active = false }
  })

  async function save() {
    try {
      const rows = await api(`/api/catalog/items/${output.id}/recipe`, { method: 'PUT', body: JSON.stringify({ components: lines ?? [] }) })
      lines = rows.map((r) => ({ componentItemId: r.componentItemId, quantity: r.quantity }))
      notifySuccess('Recette enregistrée')
    } catch (e) {
      fail(e)
    }
  }

  function setItem(i, v) {
    lines = (lines ?? []).map((l, j) => (j === i ? { ...l, componentItemId: v } : l))
  }
  function setQty(i, v) {
    lines = (lines ?? []).map((l, j) => (j === i ? { ...l, quantity: Number(v) } : l))
  }
  function removeLine(i) {
    lines = (lines ?? []).filter((_, j) => j !== i)
  }
  function addLine() {
    lines = [...(lines ?? []), { componentItemId: candidates[0].id, quantity: 1 }]
  }
</script>

{#if lines === null}
  <p class="mt-2 text-xs text-muted-foreground">Chargement…</p>
{:else}
  <div class="mt-2 flex flex-col gap-2">
    {#if lines.length === 0}<p class="text-xs text-muted-foreground">Aucun composant (item brut).</p>{/if}
    {#each lines as line, index (index)}
      <div class="flex flex-wrap items-center gap-2">
        <SelectField value={line.componentItemId} onChange={(v) => setItem(index, v)} options={candidates.map((c) => ({ value: c.id, label: c.name }))} />
        <NumberInput value={String(line.quantity)} onchange={(v) => setQty(index, v)} min={1} class="w-28" />
        <Button variant="ghost" size="icon" ariaLabel="Retirer l'ingrédient" onclick={() => removeLine(index)}><Trash2 size={16} /></Button>
      </div>
    {/each}
    <div class="flex gap-2">
      <Button variant="outline" size="sm" disabled={candidates.length === 0} onclick={addLine}>Ajouter un composant</Button>
      <Button size="sm" onclick={save}>Enregistrer la recette</Button>
    </div>
  </div>
{/if}
