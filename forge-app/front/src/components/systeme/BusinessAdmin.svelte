<script>
  import { onMount } from 'svelte'
  import { Plus } from '@lucide/svelte'
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
  import Modal from '../ui/Modal.svelte'

  let businesses = $state([])
  let nom = $state('')
  let type = $state('FORGE')
  let showCreate = $state(false)
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
      // Le back crée un « Coffre principal » (STOCK) par défaut, atomiquement.
      await api('/api/businesses', { method: 'POST', body: JSON.stringify({ nom: nom.trim(), type }) })
      nom = ''
      showCreate = false
      notifySuccess('Business créé (coffre principal initialisé)')
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<div class="flex flex-col gap-4">
  <div class="flex justify-end">
    <Button onclick={() => (showCreate = true)}><Plus size={16} /> Nouveau business</Button>
  </div>

  <Modal bind:open={showCreate} title="Créer un business">
    <div class="flex flex-col gap-3">
      <Input placeholder="Nom du business" bind:value={nom} />
      <SelectField
        value={type}
        onChange={(v) => (type = v)}
        options={[
          { value: 'FORGE', label: 'FORGE' },
          { value: 'COMPAGNIE', label: 'COMPAGNIE' },
        ]}
      />
      <div class="flex justify-end gap-2">
        <Button variant="outline" onclick={() => (showCreate = false)}>Annuler</Button>
        <Button onclick={createBusiness}>Créer</Button>
      </div>
    </div>
  </Modal>

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
