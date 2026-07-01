<script>
  import { me } from '../../lib/session.js'
  import { canAdminBusiness } from '../../lib/roles.js'
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Modal from '../ui/Modal.svelte'
  import Button from '../ui/Button.svelte'
  import Input from '../ui/Input.svelte'
  import NumberInput from '../ui/NumberInput.svelte'

  let { businessId } = $props()
  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  let canAdmin = $derived(businessId ? canAdminBusiness($me, businessId) : false)

  let owed = $state([])
  let payTarget = $state(null)
  let payAmount = $state('')
  let payNote = $state('')

  function load() {
    if (!businessId) return
    api(`/api/businesses/${businessId}/finance/owed`).then((v) => (owed = v)).catch(fail)
  }
  $effect(() => {
    businessId
    load()
  })

  function openPay(row) {
    payTarget = row
    payAmount = String(Math.max(0, row.owed))
    payNote = ''
  }
  async function pay() {
    const amount = Math.round(Number(payAmount))
    if (!amount || amount <= 0) return notifyError('Montant invalide')
    try {
      await api(`/api/businesses/${businessId}/finance/pay`, {
        method: 'POST',
        body: JSON.stringify({ forgeronUserId: payTarget.userId, amount, note: payNote || null }),
      })
      notifySuccess('Part versée')
      payTarget = null
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<div class="overflow-auto rounded-xl border">
  <table class="w-full text-sm">
    <thead>
      <tr class="border-b bg-muted/30 text-left text-xs uppercase tracking-wide text-muted-foreground">
        <th class="px-4 py-3">Forgeron</th>
        <th class="px-4 py-3 text-right">Gagné</th>
        <th class="px-4 py-3 text-right">Versé</th>
        <th class="px-4 py-3 text-right">Reste dû</th>
        <th class="px-4 py-3 text-right">Action</th>
      </tr>
    </thead>
    <tbody>
      {#each owed as r (r.userId)}
        <tr class="border-b">
          <td class="px-4 py-3 font-medium">{r.name}</td>
          <td class="px-4 py-3 text-right tabular-nums">{fmt(r.earned)}</td>
          <td class="px-4 py-3 text-right tabular-nums text-muted-foreground">{fmt(r.paid)}</td>
          <td class="px-4 py-3 text-right font-semibold tabular-nums" style="color:{r.owed > 0 ? '#f5a06a' : '#7fd398'};">{fmt(r.owed)}</td>
          <td class="px-4 py-3 text-right">
            {#if canAdmin && r.owed > 0}
              <Button size="sm" onclick={() => openPay(r)}>Payer</Button>
            {/if}
          </td>
        </tr>
      {/each}
      {#if owed.length === 0}
        <tr><td colspan="5" class="px-4 py-6 text-center text-sm text-muted-foreground">Aucune part à verser.</td></tr>
      {/if}
    </tbody>
  </table>
</div>

<Modal open={payTarget != null} title={payTarget ? `Payer ${payTarget.name}` : 'Payer'}>
  {#if payTarget}
    <div class="flex flex-col gap-3">
      <p class="text-sm text-muted-foreground">Reste dû : <strong class="text-foreground">{fmt(payTarget.owed)} septims</strong>. Les septimes sortent du coffre.</p>
      <label class="flex flex-col gap-1 text-xs text-muted-foreground">Montant
        <NumberInput value={payAmount} onchange={(v) => (payAmount = v)} min={0} max={payTarget.owed} class="w-40" />
      </label>
      <Input placeholder="Note (optionnel)" bind:value={payNote} ariaLabel="Note" />
      <div class="flex justify-end gap-2">
        <Button variant="outline" onclick={() => (payTarget = null)}>Annuler</Button>
        <Button onclick={pay}>Verser</Button>
      </div>
    </div>
  {/if}
</Modal>
