<script>
  import { me, currentBusinessId, shift, refreshShift } from '../lib/session.js'
  import { api, ApiError } from '../lib/api.js'
  import { canOperateBusiness, canAdminBusiness } from '../lib/roles.js'
  import { notifyError, notifySuccess } from '../lib/notifications.js'
  import FactureList from '../components/billing/FactureList.svelte'
  import Pos from '../components/billing/Pos.svelte'
  import Modal from '../components/ui/Modal.svelte'
  import Button from '../components/ui/Button.svelte'

  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  let canOperate = $derived($currentBusinessId ? canOperateBusiness($me, $currentBusinessId) : false)
  let canAdmin = $derived($currentBusinessId ? canAdminBusiness($me, $currentBusinessId) : false)
  let view = $state('list')
  let editFacture = $state(null)
  let factures = $state([])
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function loadFactures() {
    const id = $currentBusinessId
    if (!id) return
    api(`/api/businesses/${id}/factures`).then((v) => (factures = v)).catch(fail)
  }
  $effect(() => {
    $currentBusinessId
    loadFactures()
  })

  async function openShift() {
    try {
      await api(`/api/businesses/${$currentBusinessId}/sessions/open`, { method: 'POST' })
      refreshShift()
    } catch (e) {
      fail(e)
    }
  }
  let recap = $state(null) // SessionDto de la session fermée → modal récap

  async function closeShift() {
    try {
      recap = await api(`/api/businesses/${$currentBusinessId}/sessions/close`, { method: 'POST' })
      refreshShift()
    } catch (e) {
      fail(e)
    }
  }
  const rnd = (n) => Math.round(Number(n ?? 0))
  let recapDuree = $derived.by(() => {
    if (!recap?.openedAt || !recap?.closedAt) return '—'
    const m = Math.max(0, Math.round((new Date(recap.closedAt) - new Date(recap.openedAt)) / 60000))
    return m >= 60 ? `${Math.floor(m / 60)}h ${m % 60}min` : `${m}min`
  })
</script>

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut) pour facturer.</p>
{:else if view === 'pos'}
  <Pos businessId={$currentBusinessId} {canOperate} edit={editFacture} onBack={() => { editFacture = null; view = 'list' }} onEmitted={() => { editFacture = null; loadFactures(); view = 'list' }} />
{:else}
  <FactureList
    businessId={$currentBusinessId}
    {factures}
    shiftOpen={!!$shift?.open}
    shiftSince={$shift?.session?.openedAt ?? null}
    {canOperate}
    {canAdmin}
    meId={$me.user.id}
    onNew={() => { editFacture = null; view = 'pos' }}
    onEdit={(f) => { editFacture = f; view = 'pos' }}
    onOpenShift={openShift}
    onCloseShift={closeShift}
    onChange={loadFactures}
  />
{/if}

<!-- Récap de fin de service : CA, bénéfice, parts, à déposer (CDC §8). -->
<Modal open={recap != null} title="Fin de service — récap">
  {#if recap}
    {@const partForgeron = rnd(recap.workerShare)}
    {@const aDeposer = Math.max(0, recap.totalSales - partForgeron)}
    <div class="flex flex-col gap-3">
      <div class="grid grid-cols-2 gap-2 text-sm">
        <span class="text-muted-foreground">Durée</span><span class="text-right font-medium">{recapDuree}</span>
        <span class="text-muted-foreground">Factures</span><span class="text-right font-medium">{recap.ordersCount}</span>
        <span class="text-muted-foreground">Chiffre d'affaires</span><span class="text-right font-medium tabular-nums">{fmt(recap.totalSales)} septims</span>
        <span class="text-muted-foreground">Bénéfice</span><span class="text-right font-medium tabular-nums">{fmt(rnd(recap.totalProfit))} septims</span>
        <span class="text-muted-foreground">Part forge</span><span class="text-right font-medium tabular-nums">{fmt(rnd(recap.businessShare))} septims</span>
        <span class="text-muted-foreground">Ta part (forgeron)</span><span class="text-right font-semibold tabular-nums" style="color:#f5a06a;">{fmt(partForgeron)} septims</span>
      </div>
      <div class="rounded-lg border p-3 text-sm" style="border-color:rgba(232,89,12,0.35);">
        <div class="flex items-center justify-between">
          <span class="font-semibold">À déposer dans le coffre</span>
          <span class="text-lg font-bold tabular-nums" style="color:#f5a06a;">{fmt(aDeposer)} septims</span>
        </div>
        <p class="mt-1 text-xs text-muted-foreground">CA encaissé en jeu − ta part. Garde ta part, dépose le reste.</p>
      </div>
      <div class="flex justify-end">
        <Button onclick={() => (recap = null)}>Fermer</Button>
      </div>
    </div>
  {/if}
</Modal>
