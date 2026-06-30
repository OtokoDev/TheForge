<script>
  import { me, currentBusinessId } from '../lib/session.js'
  import { canOperateBusiness } from '../lib/roles.js'
  import { api, ApiError } from '../lib/api.js'
  import { notifyError, notifySuccess } from '../lib/notifications.js'
  import PageHeader from '../components/PageHeader.svelte'
  import Modal from '../components/ui/Modal.svelte'
  import Button from '../components/ui/Button.svelte'
  import SelectField from '../components/ui/SelectField.svelte'
  import NumberInput from '../components/ui/NumberInput.svelte'
  import { Plus } from '@lucide/svelte'

  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  let canOperate = $derived($currentBusinessId ? canOperateBusiness($me, $currentBusinessId) : false)

  let orders = $state([])
  let craftables = $state([]) // items avec recette
  let members = $state([])

  let showCreate = $state(false)
  let outItem = $state('')
  let qty = $state('1')
  let assignee = $state('')

  const STATUS = {
    EN_ATTENTE: { label: 'En attente', color: '#9a938c', bg: 'rgba(255,255,255,0.07)' },
    EN_COURS: { label: 'En cours', color: '#f5a06a', bg: 'rgba(232,89,12,0.15)' },
    TERMINEE: { label: 'Terminée', color: '#5BBF73', bg: 'rgba(91,191,115,0.16)' },
    ANNULEE: { label: 'Annulée', color: '#ed8472', bg: 'rgba(229,96,77,0.13)' },
  }

  function load() {
    const id = $currentBusinessId
    if (!id) return
    api(`/api/businesses/${id}/production`).then((v) => (orders = v)).catch(fail)
  }
  $effect(() => {
    const id = $currentBusinessId
    if (!id) return
    api('/api/catalog/items').then((v) => (craftables = v.filter((i) => i.hasRecipe))).catch(fail)
    api(`/api/businesses/${id}/members`).then((v) => (members = v)).catch(fail)
    load()
  })

  let memberName = $derived((uid) => {
    const m = members.find((x) => x.userId === uid)
    return m ? (m.inGameName ?? m.username) : '—'
  })

  async function create() {
    if (!outItem) return notifyError('Choisis un objet à fabriquer')
    const body = { outputItemId: outItem, quantity: Math.max(1, Math.round(Number(qty) || 1)), assignedTo: assignee || null }
    try {
      await api(`/api/businesses/${$currentBusinessId}/production`, { method: 'POST', body: JSON.stringify(body) })
      notifySuccess('Ordre de fabrication créé')
      showCreate = false
      outItem = ''; qty = '1'; assignee = ''
      load()
    } catch (e) {
      fail(e)
    }
  }
  async function act(id, action, confirmMsg) {
    if (confirmMsg && !confirm(confirmMsg)) return
    try {
      await api(`/api/businesses/${$currentBusinessId}/production/${id}/${action}`, { method: 'POST' })
      notifySuccess(action === 'complete' ? 'Fabriqué — stock mis à jour' : 'Mis à jour')
      load()
    } catch (e) {
      fail(e)
    }
  }

  const dt = (iso) => (iso ? new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }) : '—')
</script>

<PageHeader title="Atelier" description="Ordres de fabrication — consomme les ingrédients, produit vers le stock." />

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut) pour gérer la production.</p>
{:else}
  <div class="flex justify-end">
    {#if canOperate}
      <Button onclick={() => (showCreate = true)}><Plus size={16} /> Nouvel ordre</Button>
    {/if}
  </div>

  <div class="mt-4 overflow-auto rounded-xl border">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b bg-muted/30 text-left text-xs uppercase tracking-wide text-muted-foreground">
          <th class="px-4 py-3">N°</th>
          <th class="px-4 py-3">Objet</th>
          <th class="px-4 py-3 text-right">Qté</th>
          <th class="px-4 py-3 text-center">Statut</th>
          <th class="px-4 py-3">Forgeron</th>
          <th class="px-4 py-3">Créé</th>
          <th class="px-4 py-3 text-right">Actions</th>
        </tr>
      </thead>
      <tbody>
        {#each orders as o (o.id)}
          {@const st = STATUS[o.status]}
          <tr class="border-b">
            <td class="px-4 py-3 font-medium tabular-nums">#{String(o.numero).padStart(4, '0')}</td>
            <td class="px-4 py-3">{o.outputItemName}</td>
            <td class="px-4 py-3 text-right tabular-nums">{o.quantity}</td>
            <td class="px-4 py-3 text-center">
              <span class="rounded-full px-2.5 py-1 text-xs font-semibold" style="color:{st.color}; background:{st.bg};">{st.label}</span>
            </td>
            <td class="px-4 py-3 text-muted-foreground">{o.assignedTo ? memberName(o.assignedTo) : '—'}</td>
            <td class="px-4 py-3 text-muted-foreground">{dt(o.createdAt)}</td>
            <td class="px-4 py-3">
              {#if canOperate}
                <div class="flex justify-end gap-2">
                  {#if o.status === 'EN_ATTENTE'}
                    <Button size="sm" variant="outline" onclick={() => act(o.id, 'start')}>Démarrer</Button>
                  {/if}
                  {#if o.status === 'EN_ATTENTE' || o.status === 'EN_COURS'}
                    <Button size="sm" onclick={() => act(o.id, 'complete', `Fabriquer ${o.quantity}× ${o.outputItemName} ? Les ingrédients seront consommés du stock.`)}>Fabriquer</Button>
                    <Button size="sm" variant="ghost" onclick={() => act(o.id, 'cancel')}>Annuler</Button>
                  {/if}
                </div>
              {/if}
            </td>
          </tr>
        {/each}
        {#if orders.length === 0}
          <tr><td colspan="7" class="px-4 py-6 text-center text-sm text-muted-foreground">Aucun ordre de fabrication.</td></tr>
        {/if}
      </tbody>
    </table>
  </div>

  <Modal bind:open={showCreate} title="Nouvel ordre de fabrication">
    <div class="flex flex-col gap-3">
      <label class="flex flex-col gap-1 text-xs text-muted-foreground">Objet à fabriquer (avec recette)
        <SelectField value={outItem} onChange={(v) => (outItem = v)} options={craftables.map((i) => ({ value: i.id, label: i.name }))} ariaLabel="Objet à fabriquer" />
      </label>
      <div class="flex flex-wrap gap-3">
        <label class="flex flex-col gap-1 text-xs text-muted-foreground">Quantité
          <NumberInput value={qty} onchange={(v) => (qty = v)} min={1} class="w-28" />
        </label>
        <label class="flex flex-1 flex-col gap-1 text-xs text-muted-foreground">Forgeron (optionnel)
          <SelectField value={assignee} onChange={(v) => (assignee = v)} options={[{ value: '', label: '—' }, ...members.map((m) => ({ value: m.userId, label: m.inGameName ?? m.username }))]} ariaLabel="Forgeron" />
        </label>
      </div>
      {#if craftables.length === 0}
        <p class="text-xs text-muted-foreground">Aucun objet n'a de recette. Configure des recettes dans le catalogue (Système).</p>
      {/if}
      <div class="flex justify-end gap-2">
        <Button variant="outline" onclick={() => (showCreate = false)}>Annuler</Button>
        <Button onclick={create}>Créer l'ordre</Button>
      </div>
    </div>
  </Modal>
{/if}
