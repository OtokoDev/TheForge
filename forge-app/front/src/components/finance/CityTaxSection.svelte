<script>
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Modal from '../ui/Modal.svelte'
  import Button from '../ui/Button.svelte'
  import NumberInput from '../ui/NumberInput.svelte'

  let { businessId } = $props()
  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  let cityTaxDue = $state(0)
  let showCityTax = $state(false)
  let cityTaxAmount = $state('')

  function load() {
    if (!businessId) return
    api(`/api/businesses/${businessId}/finance/city-tax`).then((v) => (cityTaxDue = v.due)).catch(fail)
  }
  $effect(() => {
    businessId
    load()
  })

  function openCityTax() {
    cityTaxAmount = String(Math.max(0, cityTaxDue))
    showCityTax = true
  }
  async function payCityTax() {
    const amount = Math.round(Number(cityTaxAmount))
    if (!amount || amount <= 0) return notifyError('Montant invalide')
    try {
      await api(`/api/businesses/${businessId}/finance/city-tax`, { method: 'POST', body: JSON.stringify({ amount }) })
      notifySuccess('Taxe de la ville reversée')
      showCityTax = false
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<div class="flex flex-wrap items-center justify-between gap-3 rounded-xl border p-4" style="border-color:rgba(232,89,12,0.32);">
  <div>
    <div class="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Taxe de la ville</div>
    <div class="mt-1 text-sm">Dû : <strong style="color:#f5a06a;">{fmt(cityTaxDue)} septims</strong> <span class="text-muted-foreground">(part entreprise non encore reversée)</span></div>
    <div class="mt-1 text-xs text-muted-foreground">Le taux se règle dans Configuration → Taxe.</div>
  </div>
  <Button onclick={openCityTax} disabled={cityTaxDue <= 0}>Reverser</Button>
</div>

<Modal bind:open={showCityTax} title="Reverser la taxe de la ville">
  <div class="flex flex-col gap-3">
    <p class="text-sm text-muted-foreground">Dû : <strong class="text-foreground">{fmt(cityTaxDue)} septims</strong>. Les septimes sortent du coffre (dépense historisée).</p>
    <label class="flex flex-col gap-1 text-xs text-muted-foreground">Montant
      <NumberInput value={cityTaxAmount} onchange={(v) => (cityTaxAmount = v)} min={0} class="w-40" />
    </label>
    <div class="flex justify-end gap-2">
      <Button variant="outline" onclick={() => (showCityTax = false)}>Annuler</Button>
      <Button onclick={payCityTax}>Reverser</Button>
    </div>
  </div>
</Modal>
