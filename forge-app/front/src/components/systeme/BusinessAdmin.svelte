<script>
  import { onMount } from 'svelte'
  import { api, ApiError } from '../../lib/api.js'
  import { notifySuccess, notifyError } from '../../lib/notifications.js'
  import Card from '../ui/Card.svelte'
  import CardHeader from '../ui/CardHeader.svelte'
  import CardTitle from '../ui/CardTitle.svelte'
  import CardContent from '../ui/CardContent.svelte'
  import Input from '../ui/Input.svelte'
  import Button from '../ui/Button.svelte'
  import Badge from '../ui/Badge.svelte'
  import SelectField from '../ui/SelectField.svelte'

  let businesses = $state([])
  let nom = $state('')
  let type = $state('FORGE')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  async function load() {
    try {
      businesses = await api('/api/businesses')
    } catch (e) {
      fail(e)
    }
  }
  onMount(load)

  async function createBusiness() {
    if (!nom.trim()) return
    try {
      const biz = await api('/api/businesses', { method: 'POST', body: JSON.stringify({ nom: nom.trim(), type }) })
      // Initialise un coffre principal (STOCK) défini par défaut → vente possible direct.
      try {
        const acc = await api(`/api/businesses/${biz.id}/accounts`, { method: 'POST', body: JSON.stringify({ name: 'Coffre principal', kind: 'STOCK' }) })
        await api(`/api/businesses/${biz.id}/defaults`, { method: 'PUT', body: JSON.stringify({ stockAccountId: acc.id, coffreAccountId: acc.id }) })
      } catch {
        /* business créé quand même ; le coffre se configure à la main */
      }
      nom = ''
      notifySuccess('Business créé (coffre principal initialisé)')
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<div class="flex flex-col gap-4">
  <Card>
    <CardHeader><CardTitle>Créer un business</CardTitle></CardHeader>
    <CardContent class="flex flex-wrap items-center gap-2">
      <Input class="max-w-xs" placeholder="Nom du business" bind:value={nom} />
      <SelectField
        value={type}
        onChange={(v) => (type = v)}
        options={[
          { value: 'FORGE', label: 'FORGE' },
          { value: 'COMPAGNIE', label: 'COMPAGNIE' },
        ]}
      />
      <Button onclick={createBusiness}>Créer</Button>
    </CardContent>
  </Card>

  <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
    {#each businesses as b (b.id)}
      <Card>
        <CardHeader>
          <div class="flex items-center justify-between">
            <CardTitle>{b.nom}</CardTitle>
            <Badge variant="outline">{b.type}</Badge>
          </div>
        </CardHeader>
      </Card>
    {/each}
    {#if businesses.length === 0}
      <p class="text-sm text-muted-foreground">Aucun business.</p>
    {/if}
  </div>

  <p class="text-sm text-muted-foreground">
    Membres, taxe, coffres et logo : sélectionne le business (en haut) puis ouvre <strong>Configuration</strong>.
  </p>
</div>
