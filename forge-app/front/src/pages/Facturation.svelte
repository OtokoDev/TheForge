<script>
  import { me, currentBusinessId, shift, refreshShift } from '../lib/session.js'
  import { api, ApiError } from '../lib/api.js'
  import { canOperateBusiness, canAdminBusiness } from '../lib/roles.js'
  import { notifyError, notifySuccess } from '../lib/notifications.js'
  import FactureList from '../components/billing/FactureList.svelte'
  import Pos from '../components/billing/Pos.svelte'

  const fmt = (n) => Number(n).toLocaleString('fr-FR')
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
  async function closeShift() {
    try {
      const s = await api(`/api/businesses/${$currentBusinessId}/sessions/close`, { method: 'POST' })
      notifySuccess(`Service fermé — ${s.ordersCount} facture(s), CA ${fmt(s.totalSales)} septims`)
      refreshShift()
    } catch (e) {
      fail(e)
    }
  }
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
