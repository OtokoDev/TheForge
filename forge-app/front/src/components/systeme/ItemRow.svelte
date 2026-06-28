<script>
  import { Trash2 } from '@lucide/svelte'
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess, toast } from '../../lib/notifications.js'
  import Badge from '../ui/Badge.svelte'
  import Button from '../ui/Button.svelte'
  import Input from '../ui/Input.svelte'
  import SelectField from '../ui/SelectField.svelte'
  import RecipeEditor from './RecipeEditor.svelte'

  let { item, items, families, materials, onChanged } = $props()

  const HAND_OPTIONS = [{ value: '', label: 'Mains : —' }, { value: 'ONE', label: '1 main' }, { value: 'TWO', label: '2 mains' }]
  const opts = (taxa, empty) => [{ value: '', label: empty }, ...taxa.map((t) => ({ value: t.id, label: t.nom }))]

  let editing = $state(false)
  let openRecipe = $state(false)
  let name = $state(item.name)
  let familyId = $state(item.familyId ?? '')
  let materialId = $state(item.materialId ?? '')
  let hand = $state(item.handRequired ?? '')

  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  const onErr = (e) => {
    if (e instanceof ApiError && e.status === 409) {
      toast(e.message, 'info')
      onChanged()
    } else fail(e)
  }

  async function put(body) {
    await api(`/api/catalog/items/${item.id}`, { method: 'PUT', body: JSON.stringify(body) })
    onChanged()
  }
  async function saveEdit() {
    try {
      await put({ name: name.trim(), familyId: familyId || null, materialId: materialId || null, handRequired: hand || null, active: item.active, version: item.version })
      editing = false
      notifySuccess('Item mis à jour')
    } catch (e) {
      onErr(e)
    }
  }
  async function toggleActive() {
    try {
      await put({ name: item.name, familyId: item.familyId, materialId: item.materialId, handRequired: item.handRequired, active: !item.active, version: item.version })
    } catch (e) {
      onErr(e)
    }
  }
  async function remove() {
    try {
      await api(`/api/catalog/items/${item.id}`, { method: 'DELETE' })
      notifySuccess('Item supprimé')
      onChanged()
    } catch (e) {
      fail(e)
    }
  }
</script>

<div class="rounded-md border bg-card px-3 py-2">
  <div class="flex flex-wrap items-center gap-2">
    <span class="font-medium">{item.name}</span>
    {#if item.familyName}
      <span class="inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium" style={item.familyColor ? `border-color:${item.familyColor}; color:${item.familyColor}` : ''}>{item.familyName}</span>
    {/if}
    {#if item.materialName}
      <span class="inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium" style={item.materialColor ? `border-color:${item.materialColor}; color:${item.materialColor}` : ''}>{item.materialName}</span>
    {/if}
    {#if item.handRequired}<Badge variant="outline">{item.handRequired === 'TWO' ? '2 mains' : '1 main'}</Badge>{/if}
    {#if item.system}<Badge variant="secondary">système</Badge>{/if}
    {#if !item.active}<Badge variant="destructive">inactif</Badge>{/if}
    <div class="flex-1"></div>
    <Button variant="ghost" size="sm" onclick={() => (openRecipe = !openRecipe)}>{openRecipe ? 'Masquer recette' : 'Recette'}</Button>
    {#if !item.system}
      <Button variant="ghost" size="sm" onclick={() => (editing = !editing)}>{editing ? 'Annuler' : 'Éditer'}</Button>
      <Button variant="ghost" size="sm" onclick={toggleActive}>{item.active ? 'Désactiver' : 'Activer'}</Button>
      <Button variant="ghost" size="icon" ariaLabel="Supprimer l'objet" onclick={remove}><Trash2 size={16} /></Button>
    {/if}
  </div>

  {#if editing}
    <div class="mt-2 flex flex-wrap items-center gap-2 border-t pt-2">
      <Input class="max-w-xs" bind:value={name} />
      <SelectField value={familyId} onChange={(v) => (familyId = v)} options={opts(families, 'Famille : —')} />
      <SelectField value={materialId} onChange={(v) => (materialId = v)} options={opts(materials, 'Matériau : —')} />
      <SelectField value={hand} onChange={(v) => (hand = v)} options={HAND_OPTIONS} />
      <Button size="sm" onclick={saveEdit}>Enregistrer</Button>
    </div>
  {/if}

  {#if openRecipe}
    <div class="border-t pt-2"><RecipeEditor output={item} {items} /></div>
  {/if}
</div>
