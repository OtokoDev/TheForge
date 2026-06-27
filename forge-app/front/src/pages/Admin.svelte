<script>
  import { onMount } from 'svelte'
  import { api, ApiError } from '../lib/api.js'
  import { notifySuccess, notifyError } from '../lib/notifications.js'
  import PageHeader from '../components/PageHeader.svelte'
  import Card from '../components/ui/Card.svelte'
  import CardHeader from '../components/ui/CardHeader.svelte'
  import CardTitle from '../components/ui/CardTitle.svelte'
  import CardContent from '../components/ui/CardContent.svelte'
  import Input from '../components/ui/Input.svelte'
  import Button from '../components/ui/Button.svelte'
  import Badge from '../components/ui/Badge.svelte'
  import SelectField from '../components/ui/SelectField.svelte'

  let businesses = $state([])
  let nom = $state('')
  let type = $state('FORGE')

  function fail(e) {
    notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  }

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
      await api('/api/businesses', { method: 'POST', body: JSON.stringify({ nom: nom.trim(), type }) })
      nom = ''
      notifySuccess('Business créé')
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<PageHeader title="Administration" description="Création et liste des business." />

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
  Membres, taxe et logo : sélectionne le business (en haut) puis ouvre <strong>Configuration</strong>.
</p>
