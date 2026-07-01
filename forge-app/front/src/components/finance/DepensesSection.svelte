<script>
  import { me } from '../../lib/session.js'
  import { canAdminBusiness } from '../../lib/roles.js'
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Modal from '../ui/Modal.svelte'
  import Button from '../ui/Button.svelte'
  import Input from '../ui/Input.svelte'
  import NumberInput from '../ui/NumberInput.svelte'
  import { Plus } from '@lucide/svelte'

  let { businessId } = $props()
  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  const dt = (iso) => new Date(iso).toLocaleDateString('fr-FR')
  let canAdmin = $derived(businessId ? canAdminBusiness($me, businessId) : false)

  let expenses = $state([])
  let showExpense = $state(false)
  let expLabel = $state('')
  let expAmount = $state('')
  let expCategory = $state('')

  function load() {
    if (!businessId) return
    api(`/api/businesses/${businessId}/finance/expenses`).then((v) => (expenses = v)).catch(fail)
  }
  $effect(() => {
    businessId
    load()
  })

  async function addExpense() {
    const amount = Math.round(Number(expAmount))
    if (!expLabel.trim()) return notifyError('Libellé requis')
    if (!amount || amount <= 0) return notifyError('Montant invalide')
    try {
      await api(`/api/businesses/${businessId}/finance/expenses`, {
        method: 'POST',
        body: JSON.stringify({ label: expLabel.trim(), amount, category: expCategory || null }),
      })
      notifySuccess('Dépense enregistrée')
      showExpense = false
      expLabel = ''; expAmount = ''; expCategory = ''
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<div class="flex flex-col gap-2">
  {#if canAdmin}
    <div class="flex justify-end">
      <Button size="sm" onclick={() => (showExpense = true)}><Plus size={15} /> Ajouter</Button>
    </div>
  {/if}
  <div class="overflow-auto rounded-xl border">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b bg-muted/30 text-left text-xs uppercase tracking-wide text-muted-foreground">
          <th class="px-4 py-3">Date</th>
          <th class="px-4 py-3">Libellé</th>
          <th class="px-4 py-3">Catégorie</th>
          <th class="px-4 py-3 text-right">Montant</th>
        </tr>
      </thead>
      <tbody>
        {#each expenses as x (x.id)}
          <tr class="border-b">
            <td class="px-4 py-3 text-muted-foreground">{dt(x.createdAt)}</td>
            <td class="px-4 py-3">{x.label}</td>
            <td class="px-4 py-3 text-muted-foreground">{x.category ?? '—'}</td>
            <td class="px-4 py-3 text-right tabular-nums">{fmt(x.amount)}</td>
          </tr>
        {/each}
        {#if expenses.length === 0}
          <tr><td colspan="4" class="px-4 py-6 text-center text-sm text-muted-foreground">Aucune dépense.</td></tr>
        {/if}
      </tbody>
    </table>
  </div>
</div>

<Modal bind:open={showExpense} title="Nouvelle dépense">
  <div class="flex flex-col gap-3">
    <Input placeholder="Libellé" bind:value={expLabel} ariaLabel="Libellé" />
    <div class="flex flex-wrap gap-3">
      <label class="flex flex-col gap-1 text-xs text-muted-foreground">Montant
        <NumberInput value={expAmount} onchange={(v) => (expAmount = v)} min={0} class="w-32" />
      </label>
      <label class="flex flex-1 flex-col gap-1 text-xs text-muted-foreground">Catégorie (optionnel)
        <Input placeholder="ex. matières, loyer…" bind:value={expCategory} ariaLabel="Catégorie" />
      </label>
    </div>
    <div class="flex justify-end gap-2">
      <Button variant="outline" onclick={() => (showExpense = false)}>Annuler</Button>
      <Button onclick={addExpense}>Enregistrer</Button>
    </div>
  </div>
</Modal>
