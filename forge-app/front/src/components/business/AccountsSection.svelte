<script>
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
  let name = $state('')
  let kind = $state('COFFRE')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function load() {
    api(`/api/businesses/${businessId}/accounts`).then((v) => (accounts = v)).catch(fail)
  }
  $effect(() => {
    businessId
    load()
  })

  async function create() {
    if (!name.trim()) return notifyError('Nom du compte requis')
    try {
      await api(`/api/businesses/${businessId}/accounts`, { method: 'POST', body: JSON.stringify({ name: name.trim(), kind }) })
      name = ''
      notifySuccess('Compte créé')
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<Card>
  <CardHeader><CardTitle>Coffres &amp; comptes</CardTitle></CardHeader>
  <CardContent class="flex flex-col gap-3">
    <div class="flex flex-wrap items-center gap-2 border-b pb-3">
      <Input class="max-w-xs" placeholder="Nom (ex. Coffre principal)" bind:value={name} />
      <SelectField
        value={kind}
        onChange={(v) => (kind = v)}
        options={[
          { value: 'COFFRE', label: 'Coffre (septims)' },
          { value: 'STOCK', label: 'Stock' },
          { value: 'AUTRE', label: 'Autre' },
        ]}
      />
      <Button onclick={create}>Créer</Button>
    </div>
    {#if accounts.length === 0}
      <p class="text-sm text-muted-foreground">Aucun compte.</p>
    {:else}
      {#each accounts as a (a.id)}
        <div class="flex items-center justify-between">
          <span class="text-sm">{a.name}</span>
          <Badge variant="outline">{KIND_LABEL[a.kind]}</Badge>
        </div>
      {/each}
    {/if}
  </CardContent>
</Card>
