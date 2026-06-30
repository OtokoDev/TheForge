<script>
  import { me, currentBusinessId } from '../lib/session.js'
  import { canAdminBusiness } from '../lib/roles.js'
  import { api, ApiError } from '../lib/api.js'
  import { notifyError, notifySuccess } from '../lib/notifications.js'
  import PageHeader from '../components/PageHeader.svelte'
  import Modal from '../components/ui/Modal.svelte'
  import Button from '../components/ui/Button.svelte'
  import Input from '../components/ui/Input.svelte'
  import NumberInput from '../components/ui/NumberInput.svelte'
  import { Plus } from '@lucide/svelte'

  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  let canAdmin = $derived($currentBusinessId ? canAdminBusiness($me, $currentBusinessId) : false)

  let summary = $state(null)
  let owed = $state([])
  let expenses = $state([])

  // Paie
  let payTarget = $state(null)
  let payAmount = $state('')
  let payNote = $state('')
  // Dépense
  let showExpense = $state(false)
  let expLabel = $state('')
  let expAmount = $state('')
  let expCategory = $state('')

  function load() {
    const id = $currentBusinessId
    if (!id) return
    api(`/api/businesses/${id}/finance/summary`).then((v) => (summary = v)).catch(fail)
    api(`/api/businesses/${id}/finance/owed`).then((v) => (owed = v)).catch(fail)
    api(`/api/businesses/${id}/finance/expenses`).then((v) => (expenses = v)).catch(fail)
  }
  $effect(() => {
    $currentBusinessId
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
      await api(`/api/businesses/${$currentBusinessId}/finance/pay`, {
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
  async function addExpense() {
    const amount = Math.round(Number(expAmount))
    if (!expLabel.trim()) return notifyError('Libellé requis')
    if (!amount || amount <= 0) return notifyError('Montant invalide')
    try {
      await api(`/api/businesses/${$currentBusinessId}/finance/expenses`, {
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

  const dt = (iso) => new Date(iso).toLocaleDateString('fr-FR')
</script>

<PageHeader title="Finance" description="Paie des forgerons, dépenses et compte de résultat." />

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut).</p>
{:else}
  <!-- Résultat -->
  <div class="grid grid-cols-[repeat(auto-fit,minmax(150px,1fr))] gap-3">
    {@render kpi('Encaissé', summary?.caEncaisse)}
    {@render kpi('Coût matières', summary?.coutTotal)}
    {@render kpi('Bénéfice', summary?.benefice)}
    {@render kpi('Part entreprise', summary?.partBusiness)}
    {@render kpi('Paie versée', summary?.paieVersee)}
    {@render kpi('Dépenses', summary?.depenses)}
    {@render kpi('Résultat', summary?.resultat, true)}
  </div>

  <!-- Paie -->
  <div class="mt-6">
    <h2 class="mb-2 text-lg font-semibold">Paie des forgerons</h2>
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
  </div>

  <!-- Dépenses -->
  <div class="mt-6">
    <div class="mb-2 flex items-center justify-between">
      <h2 class="text-lg font-semibold">Dépenses</h2>
      {#if canAdmin}<Button size="sm" onclick={() => (showExpense = true)}><Plus size={15} /> Ajouter</Button>{/if}
    </div>
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

  <!-- Modal paie -->
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

  <!-- Modal dépense -->
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
{/if}

{#snippet kpi(label, value, accent = false)}
  <div class="rounded-xl border p-4" style={accent ? 'border-color:rgba(232,89,12,0.32);' : ''}>
    <div class="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</div>
    <div class="mt-1 text-xl font-bold tabular-nums" style={accent ? 'color:#f5a06a;' : ''}>{fmt(value)} <span class="text-xs font-normal text-muted-foreground">septims</span></div>
  </div>
{/snippet}
