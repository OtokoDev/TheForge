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
  let history = $state([])
  let forgeronPct = $state('')
  let cityFixed = $state('')
  let cityPct = $state('')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function load() {
    api(`/api/businesses/${businessId}/tax-rate`).then((v) => {
      forgeronPct = String(+((v.rate ?? 0) * 100).toFixed(2))
      cityFixed = String(v.cityFixed ?? 0)
      cityPct = String(+((v.cityRate ?? 0) * 100).toFixed(2))
    }).catch(fail)
    api(`/api/businesses/${businessId}/tax-rate/history`).then((v) => (history = v)).catch(fail)
  }
  $effect(() => {
    businessId
    load()
  })

  async function save() {
    const forgeron = Number(forgeronPct)
    const cRate = Number(cityPct)
    const cFixed = Math.max(0, Math.round(Number(cityFixed) || 0))
    if (Number.isNaN(forgeron) || forgeron < 0 || forgeron > 100) return notifyError('Part forgeron entre 0 et 100 %')
    if (Number.isNaN(cRate) || cRate < 0 || cRate > 100) return notifyError('Taxe ville % entre 0 et 100')
    try {
      await api(`/api/businesses/${businessId}/tax-rate`, {
        method: 'PUT',
        body: JSON.stringify({ rate: forgeron / 100, cityFixed: cFixed, cityRate: cRate / 100 }),
      })
      notifySuccess('Fiscalité mise à jour')
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<Card>
  <CardHeader><CardTitle>Fiscalité</CardTitle></CardHeader>
  <CardContent class="flex flex-col gap-4">
    <div class="flex flex-col gap-1">
      <div class="text-sm font-semibold">Part forgeron</div>
      <p class="text-sm text-muted-foreground">
        Fraction du <strong class="text-foreground">prix de vente</strong> reversée au forgeron
        (récap dans Finance → Paie). La forge garde le reste du bénéfice.
      </p>
      <label class="mt-1 flex flex-col gap-1 text-xs text-muted-foreground">Part forgeron (%)
        <NumberInput value={forgeronPct} onchange={(v) => (forgeronPct = v)} min={0} max={100} class="w-32" />
      </label>
    </div>

    <div class="flex flex-col gap-1 border-t pt-3">
      <div class="text-sm font-semibold">Taxe de la ville</div>
      <p class="text-sm text-muted-foreground">
        À reverser ~1×/semaine (Finance → Taxe). Due = <strong class="text-foreground">forfait hebdo</strong>
        + <strong class="text-foreground">% du CA après paie des forgerons</strong>.
      </p>
      <div class="mt-1 flex flex-wrap gap-3">
        <label class="flex flex-col gap-1 text-xs text-muted-foreground">Forfait hebdo (septims)
          <NumberInput value={cityFixed} onchange={(v) => (cityFixed = v)} min={0} class="w-36" />
        </label>
        <label class="flex flex-col gap-1 text-xs text-muted-foreground">Taux (% du CA après paie)
          <NumberInput value={cityPct} onchange={(v) => (cityPct = v)} min={0} max={100} class="w-36" />
        </label>
      </div>
    </div>

    <div><Button onclick={save}>Enregistrer</Button></div>

    {#if history.length > 0}
      <div class="flex flex-col gap-1 border-t pt-2">
        <span class="text-xs font-medium text-muted-foreground">Historique part forgeron</span>
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
