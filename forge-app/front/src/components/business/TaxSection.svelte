<script>
  import { api, ApiError } from '../../lib/api.js'
  import { formatDate } from '../../lib/format.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Card from '../ui/Card.svelte'
  import CardHeader from '../ui/CardHeader.svelte'
  import CardTitle from '../ui/CardTitle.svelte'
  import CardContent from '../ui/CardContent.svelte'
  import NumberInput from '../ui/NumberInput.svelte'
  import Button from '../ui/Button.svelte'

  let { businessId } = $props()
  let current = $state(null)
  let history = $state([])
  let pct = $state('')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function load() {
    api(`/api/businesses/${businessId}/tax-rate`).then((v) => (current = v)).catch(fail)
    api(`/api/businesses/${businessId}/tax-rate/history`).then((v) => (history = v)).catch(fail)
  }
  $effect(() => {
    businessId
    load()
  })

  async function save() {
    const value = Number(pct)
    if (pct === '' || Number.isNaN(value) || value < 0 || value > 100) return notifyError('Taux entre 0 et 100 %')
    try {
      await api(`/api/businesses/${businessId}/tax-rate`, { method: 'PUT', body: JSON.stringify({ rate: value / 100 }) })
      pct = ''
      notifySuccess('Taux mis à jour')
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<Card>
  <CardHeader><CardTitle>Taxe</CardTitle></CardHeader>
  <CardContent class="flex flex-col gap-3">
    <p class="text-sm">Taux courant : <strong>{current ? `${(current.rate * 100).toFixed(2)} %` : '…'}</strong></p>
    <div class="flex flex-wrap items-center gap-2">
      <NumberInput value={pct} onchange={(v) => (pct = v)} min={0} max={100} placeholder="%" class="w-40" />
      <span class="text-sm text-muted-foreground">%</span>
      <Button onclick={save}>Définir</Button>
    </div>
    {#if history.length > 0}
      <div class="flex flex-col gap-1 border-t pt-2">
        <span class="text-xs font-medium text-muted-foreground">Historique</span>
        {#each history as h, i (i)}
          <div class="flex justify-between text-xs text-muted-foreground">
            <span>{(h.rate * 100).toFixed(2)} %</span>
            <span>{formatDate(h.validFrom)} → {h.validTo ? formatDate(h.validTo) : 'en cours'}</span>
          </div>
        {/each}
      </div>
    {/if}
  </CardContent>
</Card>
