<script>
  import { Star } from '@lucide/svelte'
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

  let { businessId } = $props()
  const KIND_LABEL = { COFFRE: 'Coffre', STOCK: 'Stock', AUTRE: 'Autre' }
  let accounts = $state([])
  let defaultId = $state(null)
  let name = $state('')
  let kind = $state('STOCK')
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
      notifySuccess('Coffre créé')
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
    <div class="flex flex-wrap items-center gap-2 border-b pb-3">
      <Input class="max-w-xs" placeholder="Nom (ex. Coffre principal)" bind:value={name} />
      <SelectField value={kind} onChange={(v) => (kind = v)} options={[{ value: 'STOCK', label: 'Stock' }, { value: 'AUTRE', label: 'Autre' }]} />
      <Button onclick={create}>Créer</Button>
    </div>
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
          <Badge variant="outline">{KIND_LABEL[a.kind] ?? a.kind}</Badge>
        </div>
      {/each}
    {/if}
  </CardContent>
</Card>
