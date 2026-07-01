<script>
  import { me, currentBusinessId } from '../lib/session.js'
  import { canOperateBusiness } from '../lib/roles.js'
  import { api, ApiError } from '../lib/api.js'
  import { notifyError, notifySuccess } from '../lib/notifications.js'
  import PageHeader from '../components/PageHeader.svelte'
  import Modal from '../components/ui/Modal.svelte'
  import Button from '../components/ui/Button.svelte'
  import Input from '../components/ui/Input.svelte'
  import NumberInput from '../components/ui/NumberInput.svelte'
  import { Trash2, Plus } from '@lucide/svelte'

  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  let canOperate = $derived($currentBusinessId ? canOperateBusiness($me, $currentBusinessId) : false)

  let commandes = $state([])
  let items = $state([])
  let prices = $state(new Map())
  let statusFilter = $state('all')
  let open = $state(null)

  // Création
  let showCreate = $state(false)
  let clientName = $state('')
  let clientNote = $state('')
  let dueDate = $state('')
  let acompte = $state('')
  let draftLines = $state([]) // [{ itemId, quantity, unitPrice }]
  let pickQuery = $state('')

  const STATUS = {
    DEVIS: { label: 'Devis', color: '#9a938c', bg: 'rgba(255,255,255,0.07)' },
    CONFIRMEE: { label: 'Confirmée', color: '#7aa7ff', bg: 'rgba(122,167,255,0.14)' },
    EN_PRODUCTION: { label: 'En production', color: '#f5a06a', bg: 'rgba(232,89,12,0.15)' },
    PRETE: { label: 'Prête', color: '#7fd398', bg: 'rgba(91,191,115,0.13)' },
    LIVREE: { label: 'Livrée', color: '#5BBF73', bg: 'rgba(91,191,115,0.18)' },
    ANNULEE: { label: 'Annulée', color: '#ed8472', bg: 'rgba(229,96,77,0.13)' },
  }
  const NEXT = { DEVIS: 'CONFIRMEE', CONFIRMEE: 'EN_PRODUCTION', EN_PRODUCTION: 'PRETE' }
  const FILTERS = [
    { id: 'all', l: 'Toutes' },
    { id: 'ouvertes', l: 'En cours' },
    { id: 'DEVIS', l: 'Devis' },
    { id: 'LIVREE', l: 'Livrées' },
  ]

  function load() {
    const id = $currentBusinessId
    if (!id) return
    api(`/api/businesses/${id}/commandes`).then((v) => (commandes = v)).catch(fail)
  }
  $effect(() => {
    const id = $currentBusinessId
    if (!id) return
    api('/api/catalog/items').then((v) => (items = v)).catch(fail)
    api(`/api/businesses/${id}/products`)
      .then((rows) => (prices = new Map(rows.filter((r) => r.prixRevente != null).map((r) => [r.itemId, r.prixRevente]))))
      .catch(fail)
    load()
  })

  let itemName = $derived(new Map(items.map((i) => [i.id, i.name])))
  let rows = $derived(
    commandes.filter((c) => {
      if (statusFilter === 'all') return true
      if (statusFilter === 'ouvertes') return c.status !== 'LIVREE' && c.status !== 'ANNULEE'
      return c.status === statusFilter
    }),
  )

  let catalogue = $derived.by(() => {
    const q = pickQuery.trim().toLowerCase()
    return q === '' ? items : items.filter((i) => i.name.toLowerCase().includes(q))
  })
  function addToCart(it) {
    const existing = draftLines.find((l) => l.itemId === it.id)
    if (existing) {
      draftLines = draftLines.map((l) => (l.itemId === it.id ? { ...l, quantity: l.quantity + 1 } : l))
    } else {
      draftLines = [...draftLines, { itemId: it.id, quantity: 1, unitPrice: Number(prices.get(it.id) ?? 0) }]
    }
  }
  function incLine(itemId, d) {
    draftLines = draftLines.map((l) => (l.itemId === itemId ? { ...l, quantity: l.quantity + d } : l)).filter((l) => l.quantity > 0)
  }
  const draftTotal = $derived(draftLines.reduce((s, l) => s + Number(l.unitPrice) * l.quantity, 0))

  function resetForm() {
    clientName = ''; clientNote = ''; dueDate = ''; acompte = ''; draftLines = []; pickQuery = ''
  }

  async function createCommande() {
    if (draftLines.length === 0) return notifyError('Ajoute au moins une ligne')
    const body = {
      clientName: clientName || null,
      clientNote: clientNote || null,
      dueDate: dueDate ? new Date(dueDate).toISOString() : null,
      acompte: acompte === '' ? null : Math.max(0, Math.round(Number(acompte))),
      lines: draftLines.map((l) => ({ itemId: l.itemId, quantity: l.quantity, unitPrice: Number(l.unitPrice) })),
    }
    try {
      await api(`/api/businesses/${$currentBusinessId}/commandes`, { method: 'POST', body: JSON.stringify(body) })
      notifySuccess('Commande créée')
      showCreate = false
      resetForm()
      load()
    } catch (e) {
      fail(e)
    }
  }

  async function setStatus(id, status) {
    try {
      await api(`/api/businesses/${$currentBusinessId}/commandes/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) })
      load()
    } catch (e) {
      fail(e)
    }
  }
  async function livrer(id) {
    if (!confirm('Livrer la commande ? Une facture brouillon sera créée (à valider en Facturation).')) return
    try {
      const f = await api(`/api/businesses/${$currentBusinessId}/commandes/${id}/facture`, { method: 'POST' })
      notifySuccess(`Facture #${String(f.numero).padStart(4, '0')} créée — à valider en Facturation`)
      load()
    } catch (e) {
      fail(e)
    }
  }
  async function del(id) {
    if (!confirm('Supprimer ce devis ?')) return
    try {
      await api(`/api/businesses/${$currentBusinessId}/commandes/${id}`, { method: 'DELETE' })
      notifySuccess('Commande supprimée')
      load()
    } catch (e) {
      fail(e)
    }
  }

  const dt = (iso) => (iso ? new Date(iso).toLocaleDateString('fr-FR') : '—')
</script>

<PageHeader title="Commandes" description="Devis, suivi de production et livraison vers facture." />

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut) pour gérer les commandes.</p>
{:else}
  <div class="flex flex-wrap items-center justify-between gap-3">
    <div class="flex flex-wrap gap-2">
      {#each FILTERS as f (f.id)}
        <button
          onclick={() => (statusFilter = f.id)}
          class="rounded-full border px-3 py-1 text-sm font-medium transition {statusFilter === f.id
            ? 'border-primary bg-primary text-primary-foreground'
            : 'border-border bg-muted/40 text-muted-foreground hover:text-foreground'}"
        >{f.l}</button>
      {/each}
    </div>
    {#if canOperate}
      <Button onclick={() => (showCreate = true)}><Plus size={16} /> Nouvelle commande</Button>
    {/if}
  </div>

  <div class="mt-4 overflow-auto rounded-xl border">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b bg-muted/30 text-left text-xs uppercase tracking-wide text-muted-foreground">
          <th class="px-4 py-3">N°</th>
          <th class="px-4 py-3">Client</th>
          <th class="px-4 py-3">Articles</th>
          <th class="px-4 py-3 text-center">Statut</th>
          <th class="px-4 py-3">Échéance</th>
          <th class="px-4 py-3 text-right">Total</th>
        </tr>
      </thead>
      <tbody>
        {#each rows as c (c.id)}
          {@const st = STATUS[c.status]}
          <tr onclick={() => (open = open === c.id ? null : c.id)} class="cursor-pointer border-b transition hover:bg-muted/20">
            <td class="px-4 py-3 font-medium tabular-nums">#{String(c.numero).padStart(4, '0')}</td>
            <td class="px-4 py-3">{c.clientName ?? 'Client de passage'}</td>
            <td class="max-w-[260px] truncate px-4 py-3 text-muted-foreground">{c.lines.map((l) => `${l.quantity}× ${l.itemName}`).join(', ')}</td>
            <td class="px-4 py-3 text-center">
              <span class="rounded-full px-2.5 py-1 text-xs font-semibold" style="color:{st.color}; background:{st.bg};">{st.label}</span>
            </td>
            <td class="px-4 py-3 text-muted-foreground">{dt(c.dueDate)}</td>
            <td class="px-4 py-3 text-right font-semibold tabular-nums">{fmt(c.total)}</td>
          </tr>
          {#if open === c.id}
            <tr class="border-b bg-muted/10">
              <td colspan="6" class="px-4 py-3">
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <div class="text-sm text-muted-foreground">
                    {c.lines.map((l) => `${l.quantity}× ${l.itemName} (${fmt(l.unitPrice * l.quantity)})`).join(' · ')}
                    {#if c.acompte > 0}<span> — acompte {fmt(c.acompte)} septims</span>{/if}
                    {#if c.factureId}<span class="text-emerald-400"> — facturée</span>{/if}
                  </div>
                  {#if canOperate}
                    <div class="flex flex-wrap gap-2" role="presentation" onclick={(e) => e.stopPropagation()}>
                      {#if NEXT[c.status]}
                        <Button size="sm" variant="outline" onclick={() => setStatus(c.id, NEXT[c.status])}>→ {STATUS[NEXT[c.status]].label}</Button>
                      {/if}
                      {#if c.status !== 'LIVREE' && c.status !== 'ANNULEE' && c.status !== 'DEVIS' && !c.factureId}
                        <Button size="sm" onclick={() => livrer(c.id)}>Livrer → facture</Button>
                      {/if}
                      {#if c.status !== 'LIVREE' && c.status !== 'ANNULEE'}
                        <Button size="sm" variant="ghost" onclick={() => setStatus(c.id, 'ANNULEE')}>Annuler</Button>
                      {/if}
                      {#if !c.factureId}
                        <Button size="sm" variant="ghost" ariaLabel="Supprimer" onclick={() => del(c.id)}><Trash2 size={15} /></Button>
                      {/if}
                    </div>
                  {/if}
                </div>
              </td>
            </tr>
          {/if}
        {/each}
        {#if rows.length === 0}
          <tr><td colspan="6" class="px-4 py-6 text-center text-sm text-muted-foreground">Aucune commande.</td></tr>
        {/if}
      </tbody>
    </table>
  </div>

  <Modal bind:open={showCreate} title="Nouvelle commande">
    <div class="flex flex-col gap-3">
      <Input placeholder="Client (pseudo RP)" bind:value={clientName} ariaLabel="Client" />
      <Input placeholder="Note (optionnel)" bind:value={clientNote} ariaLabel="Note" />
      <div class="flex flex-wrap gap-3">
        <label class="flex flex-col gap-1 text-xs text-muted-foreground">Échéance
          <Input type="date" bind:value={dueDate} ariaLabel="Échéance" class="w-44" />
        </label>
        <label class="flex flex-col gap-1 text-xs text-muted-foreground">Acompte
          <NumberInput value={acompte} onchange={(v) => (acompte = v)} min={0} placeholder="0" class="w-32" />
        </label>
      </div>

      <div class="flex flex-col gap-2 border-t pt-3">
        <input
          bind:value={pickQuery}
          placeholder="Chercher un article à ajouter…"
          aria-label="Chercher un article"
          class="rounded-md border bg-input/30 px-2.5 py-1.5 text-sm outline-none"
        />
        <div class="grid max-h-40 grid-cols-2 gap-2 overflow-auto sm:grid-cols-3">
          {#each catalogue as it (it.id)}
            <button
              type="button"
              onclick={() => addToCart(it)}
              class="flex flex-col items-start gap-0.5 rounded-lg border bg-card/60 p-2 text-left transition hover:border-primary hover:bg-muted"
            >
              <span class="w-full truncate text-sm font-medium">{it.name}</span>
              <span class="text-xs text-muted-foreground">{fmt(prices.get(it.id) ?? 0)} septims/u</span>
            </button>
          {/each}
          {#if catalogue.length === 0}
            <p class="col-span-full py-2 text-center text-sm text-muted-foreground">Aucun article.</p>
          {/if}
        </div>
      </div>

      {#if draftLines.length > 0}
        <div class="flex flex-col gap-1.5 rounded-lg border p-2">
          {#each draftLines as l (l.itemId)}
            <div class="flex items-center gap-2 text-sm">
              <span class="min-w-0 flex-1 truncate">{itemName.get(l.itemId) ?? '?'}</span>
              <div class="flex items-center gap-1">
                <button type="button" onclick={() => incLine(l.itemId, -1)} aria-label="Moins" class="flex size-6 items-center justify-center rounded border text-muted-foreground hover:bg-muted">−</button>
                <span class="w-7 text-center font-medium tabular-nums">{l.quantity}</span>
                <button type="button" onclick={() => incLine(l.itemId, 1)} aria-label="Plus" class="flex size-6 items-center justify-center rounded border text-muted-foreground hover:bg-muted">+</button>
              </div>
              <NumberInput value={String(l.unitPrice)} onchange={(v) => (draftLines = draftLines.map((x) => (x.itemId === l.itemId ? { ...x, unitPrice: Number(v) } : x)))} min={0} class="w-24" />
              <span class="text-xs text-muted-foreground">/u</span>
              <span class="w-20 text-right font-medium tabular-nums">{fmt(l.unitPrice * l.quantity)}</span>
              <button type="button" onclick={() => (draftLines = draftLines.filter((x) => x.itemId !== l.itemId))} aria-label="Retirer" class="text-muted-foreground hover:text-destructive"><Trash2 size={15} /></button>
            </div>
          {/each}
          <div class="mt-1 border-t pt-1 text-right text-sm font-semibold">Total : {fmt(draftTotal)} septims</div>
        </div>
      {/if}

      <div class="flex justify-end gap-2">
        <Button variant="outline" onclick={() => (showCreate = false)}>Annuler</Button>
        <Button onclick={createCommande}>Créer la commande</Button>
      </div>
    </div>
  </Modal>
{/if}
