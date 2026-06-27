<script>
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Card from '../ui/Card.svelte'
  import CardHeader from '../ui/CardHeader.svelte'
  import CardTitle from '../ui/CardTitle.svelte'
  import CardContent from '../ui/CardContent.svelte'
  import Button from '../ui/Button.svelte'
  import SelectField from '../ui/SelectField.svelte'

  let { businessId } = $props()
  let accounts = $state([])
  let stockId = $state('')
  let coffreId = $state('')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function load() {
    api(`/api/businesses/${businessId}/accounts`).then((v) => (accounts = v)).catch(fail)
    api(`/api/businesses/${businessId}/defaults`).then((d) => {
      stockId = d.stockAccountId ?? ''
      coffreId = d.coffreAccountId ?? ''
    }).catch(fail)
  }
  $effect(() => {
    businessId
    load()
  })

  const opts = (k) => [{ value: '', label: '— aucun —' }, ...accounts.filter((a) => a.kind === k).map((a) => ({ value: a.id, label: a.name }))]

  async function save() {
    try {
      await api(`/api/businesses/${businessId}/defaults`, {
        method: 'PUT',
        body: JSON.stringify({ stockAccountId: stockId || null, coffreAccountId: coffreId || null }),
      })
      notifySuccess('Comptes par défaut enregistrés')
    } catch (e) {
      fail(e)
    }
  }
</script>

<Card>
  <CardHeader><CardTitle>Comptes par défaut (caisse)</CardTitle></CardHeader>
  <CardContent class="flex flex-col gap-3">
    <p class="text-sm text-muted-foreground">
      Utilisés par la caisse : la marchandise sort du compte stock, les septims entrent dans le coffre.
    </p>
    <label class="flex flex-col gap-1 text-sm">
      Compte stock
      <SelectField value={stockId} onChange={(v) => (stockId = v)} options={opts('STOCK')} />
    </label>
    <label class="flex flex-col gap-1 text-sm">
      Coffre
      <SelectField value={coffreId} onChange={(v) => (coffreId = v)} options={opts('COFFRE')} />
    </label>
    <Button class="self-start" onclick={save}>Enregistrer</Button>
  </CardContent>
</Card>
