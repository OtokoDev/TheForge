<script>
  import { api, ApiError } from '../../lib/api.js'
  import { formatDate } from '../../lib/format.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Card from '../ui/Card.svelte'
  import CardHeader from '../ui/CardHeader.svelte'
  import CardTitle from '../ui/CardTitle.svelte'
  import CardContent from '../ui/CardContent.svelte'
  import NumberInput from '../ui/NumberInput.svelte'
  import SelectField from '../ui/SelectField.svelte'
  import Button from '../ui/Button.svelte'

  let { businessId } = $props()
  let current = $state(null)
  let history = $state([])
  let pct = $state('')
  let base = $state('PROFIT')
  const BASE_OPTS = [
    { value: 'PROFIT', label: 'Sur le bénéfice' },
    { value: 'REVENUE', label: 'Sur le chiffre d’affaires (CA)' },
  ]
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function load() {
    api(`/api/businesses/${businessId}/tax-rate`).then((v) => { current = v; base = v.base ?? 'PROFIT' }).catch(fail)
    api(`/api/businesses/${businessId}/tax-rate/history`).then((v) => (history = v)).catch(fail)
  }
  $effect(() => {
    businessId
    load()
  })

  async function save() {
    // Taux : nouveau si saisi, sinon on garde le courant (permet de ne changer que l'assiette).
    const value = pct === '' ? (current ? current.rate * 100 : 0) : Number(pct)
    if (Number.isNaN(value) || value < 0 || value > 100) return notifyError('Taux entre 0 et 100 %')
    try {
      await api(`/api/businesses/${businessId}/tax-rate`, { method: 'PUT', body: JSON.stringify({ rate: value / 100, base }) })
      pct = ''
      notifySuccess('Taxe mise à jour')
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<Card>
  <CardHeader><CardTitle>Taxe</CardTitle></CardHeader>
  <CardContent class="flex flex-col gap-3">
    <p class="text-sm text-muted-foreground">
      {#if base === 'REVENUE'}
        Prélevée sur le <strong class="text-foreground">chiffre d'affaires</strong> (montant total vendu), pas sur le bénéfice.
        Ex. : <strong class="text-foreground">10 %</strong> → l'entreprise prend 10 % du CA ; le forgeron garde le reste du bénéfice.
      {:else}
        Prélevée sur le <strong class="text-foreground">bénéfice</strong> (CA − coût des matières), pas sur le chiffre d'affaires.
        Ex. : <strong class="text-foreground">10 %</strong> → l'entreprise garde 10 % du bénéfice, le joueur 90 %.
      {/if}
    </p>
    <p class="text-sm">
      Taux courant : <strong>{current ? `${(current.rate * 100).toFixed(2)} %` : '…'}</strong>
      {#if current}<span class="text-muted-foreground"> · {base === 'REVENUE' ? 'sur le CA' : 'sur le bénéfice'}</span>{/if}
    </p>
    <div class="flex flex-wrap items-end gap-3">
      <label class="flex flex-col gap-1 text-xs text-muted-foreground">
        Assiette
        <SelectField value={base} onChange={(v) => (base = v)} options={BASE_OPTS} />
      </label>
      <label class="flex flex-col gap-1 text-xs text-muted-foreground">
        Taux (%)
        <NumberInput value={pct} onchange={(v) => (pct = v)} min={0} max={100} placeholder="%" class="w-32" />
      </label>
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
