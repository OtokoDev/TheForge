<script>
  import { Plus } from '@lucide/svelte'
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Button from '../ui/Button.svelte'
  import Input from '../ui/Input.svelte'
  import SelectField from '../ui/SelectField.svelte'
  import Modal from '../ui/Modal.svelte'
  import ItemRow from './ItemRow.svelte'

  let { items, families, materials, onChanged } = $props()

  const HAND_OPTIONS = [{ value: '', label: 'Mains : —' }, { value: 'ONE', label: '1 main' }, { value: 'TWO', label: '2 mains' }]
  const opts = (taxa, empty) => [{ value: '', label: empty }, ...taxa.map((t) => ({ value: t.id, label: t.nom }))]
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  let name = $state('')
  let familyId = $state('')
  let materialId = $state('')
  let hand = $state('')
  let query = $state('')
  let famFilter = $state('')
  let matFilter = $state('')
  let showCreate = $state(false)

  async function createItem() {
    if (!name.trim()) return
    try {
      await api('/api/catalog/items', {
        method: 'POST',
        body: JSON.stringify({ name: name.trim(), familyId: familyId || null, materialId: materialId || null, handRequired: hand || null }),
      })
      name = ''
      showCreate = false
      notifySuccess('Item créé')
      onChanged()
    } catch (e) {
      fail(e)
    }
  }

  let filtered = $derived.by(() => {
    const q = query.trim().toLowerCase()
    return items.filter(
      (i) =>
        (q === '' || i.name.toLowerCase().includes(q)) &&
        (famFilter === '' || i.familyId === famFilter) &&
        (matFilter === '' || i.materialId === matFilter),
    )
  })
</script>

<div class="flex flex-col gap-4">
  <div class="flex justify-end">
    <Button onclick={() => (showCreate = true)}><Plus size={16} /> Nouvel objet</Button>
  </div>

  <Modal bind:open={showCreate} title="Nouvel objet de base">
    <div class="flex flex-col gap-3">
      <Input placeholder="Nom de l'item" bind:value={name} />
      <SelectField value={familyId} onChange={(v) => (familyId = v)} options={opts(families, 'Famille : —')} />
      <SelectField value={materialId} onChange={(v) => (materialId = v)} options={opts(materials, 'Matériau : —')} />
      <SelectField value={hand} onChange={(v) => (hand = v)} options={HAND_OPTIONS} />
      <div class="flex justify-end gap-2">
        <Button variant="outline" onclick={() => (showCreate = false)}>Annuler</Button>
        <Button onclick={createItem}>Créer</Button>
      </div>
    </div>
  </Modal>

  <div class="flex flex-wrap items-center gap-2">
    <Input class="max-w-xs" ariaLabel="Rechercher un objet de base" placeholder="Rechercher…" bind:value={query} />
    <SelectField value={famFilter} onChange={(v) => (famFilter = v)} options={opts(families, 'Toutes familles')} />
    <SelectField value={matFilter} onChange={(v) => (matFilter = v)} options={opts(materials, 'Tous matériaux')} />
    <span class="text-sm text-muted-foreground">{filtered.length} objet(s)</span>
  </div>

  <div class="flex flex-col gap-1.5">
    {#each filtered as item (item.id)}
      <ItemRow {item} {items} {families} {materials} {onChanged} />
    {/each}
    {#if filtered.length === 0}<p class="text-sm text-muted-foreground">Aucun objet.</p>{/if}
  </div>
</div>
