<script>
  import { Star, Trash2, Plus } from '@lucide/svelte'
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Card from '../ui/Card.svelte'
  import CardHeader from '../ui/CardHeader.svelte'
  import CardTitle from '../ui/CardTitle.svelte'
  import CardContent from '../ui/CardContent.svelte'
  import Input from '../ui/Input.svelte'
  import Button from '../ui/Button.svelte'
  import Badge from '../ui/Badge.svelte'
  import SelectField from '../ui/SelectField.svelte'
  import Modal from '../ui/Modal.svelte'

  let { businessId } = $props()
  const KIND_LABEL = { COFFRE: 'Coffre', STOCK: 'Stock', AUTRE: 'Autre' }
  let accounts = $state([])
  let defaultId = $state(null)
  let name = $state('')
  let kind = $state('STOCK')
  let showCreate = $state(false)
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function load() {
    api(`/api/businesses/${businessId}/accounts`).then((v) => (accounts = v)).catch(fail)
    api(`/api/businesses/${businessId}/defaults`).then((d) => (defaultId = d.stockAccountId)).catch(fail)
  }
  $effect(() => {
    businessId
    load()
  })

  async function create() {
    if (!name.trim()) return notifyError('Nom du coffre requis')
    try {
      await api(`/api/businesses/${businessId}/accounts`, { method: 'POST', body: JSON.stringify({ name: name.trim(), kind }) })
      name = ''
      showCreate = false
      notifySuccess('Coffre créé')
      load()
    } catch (e) {
      fail(e)
    }
  }

  async function remove(a) {
    if (!window.confirm(`Supprimer le coffre « ${a.name} » ?\nIl doit être vide (toutes les quantités à 0).`)) return
    try {
      await api(`/api/businesses/${businessId}/accounts/${a.id}`, { method: 'DELETE' })
      notifySuccess('Coffre supprimé')
      load()
    } catch (e) {
      fail(e)
    }
  }

  async function setDefault(id) {
    try {
      // Un seul compte par défaut : il joue le stock ET le coffre (marchandise + septims dedans).
      await api(`/api/businesses/${businessId}/defaults`, { method: 'PUT', body: JSON.stringify({ stockAccountId: id, coffreAccountId: id }) })
      defaultId = id
      notifySuccess('Coffre principal défini')
    } catch (e) {
      fail(e)
    }
  }
</script>

<Card>
  <CardHeader><CardTitle>Coffres</CardTitle></CardHeader>
  <CardContent class="flex flex-col gap-3">
    <p class="text-sm text-muted-foreground">
      Le coffre <strong>principal</strong> (★) reçoit les ventes : marchandise et septims y sont stockés comme n'importe quel objet.
    </p>
    <div class="flex justify-end border-b pb-3">
      <Button size="sm" onclick={() => (showCreate = true)}><Plus size={15} /> Nouveau coffre</Button>
    </div>

    <Modal bind:open={showCreate} title="Nouveau coffre">
      <div class="flex flex-col gap-3">
        <Input placeholder="Nom (ex. Coffre principal)" bind:value={name} />
        <SelectField value={kind} onChange={(v) => (kind = v)} options={[{ value: 'STOCK', label: 'Stock' }, { value: 'AUTRE', label: 'Autre' }]} />
        <div class="flex justify-end gap-2">
          <Button variant="outline" onclick={() => (showCreate = false)}>Annuler</Button>
          <Button onclick={create}>Créer</Button>
        </div>
      </div>
    </Modal>
    {#if accounts.length === 0}
      <p class="text-sm text-muted-foreground">Aucun coffre. Crée-en un et désigne-le principal (★).</p>
    {:else}
      {#each accounts as a (a.id)}
        <div class="flex items-center justify-between gap-2">
          <div class="flex items-center gap-2">
            <button
              onclick={() => setDefault(a.id)}
              title={defaultId === a.id ? 'Coffre principal' : 'Définir comme principal'}
              class="transition hover:text-primary {defaultId === a.id ? 'text-primary' : 'text-muted-foreground'}"
            >
              <Star size={18} fill={defaultId === a.id ? 'currentColor' : 'none'} />
            </button>
            <span class="text-sm">{a.name}</span>
          </div>
          <div class="flex items-center gap-2">
            <Badge variant="outline">{KIND_LABEL[a.kind] ?? a.kind}</Badge>
            {#if a.id !== defaultId}
              <button onclick={() => remove(a)} title="Supprimer (si vide)" class="text-muted-foreground transition hover:text-destructive"><Trash2 size={15} /></button>
            {/if}
          </div>
        </div>
      {/each}
    {/if}
  </CardContent>
</Card>
