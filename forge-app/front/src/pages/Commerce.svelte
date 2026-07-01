<script>
  import { me, currentBusinessId } from '../lib/session.js'
  import { canOperateBusiness } from '../lib/roles.js'
  import { api, ApiError } from '../lib/api.js'
  import { onRealtime } from '../lib/realtime.js'
  import { onMount } from 'svelte'
  import { notifyError, notifySuccess } from '../lib/notifications.js'
  import PageHeader from '../components/PageHeader.svelte'
  import Modal from '../components/ui/Modal.svelte'
  import Button from '../components/ui/Button.svelte'
  import Input from '../components/ui/Input.svelte'
  import SelectField from '../components/ui/SelectField.svelte'
  import NumberInput from '../components/ui/NumberInput.svelte'
  import { Trash2, Plus } from '@lucide/svelte'

  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  const dt = (iso) => new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })

  let canOperate = $derived($currentBusinessId ? canOperateBusiness($me, $currentBusinessId) : false)

  let trades = $state([])
  let directory = $state([])
  let items = $state([])
  let open = $state(null)

  // création
  let showCreate = $state(false)
  let target = $state('')
  let septims = $state('')
  let note = $state('')
  let draftLines = $state([]) // [{ itemId, quantity }]
  let pickItem = $state('')
  let pickQty = $state('1')

  const STATUS = {
    PROPOSEE: { label: 'Proposée', color: '#7aa7ff', bg: 'rgba(122,167,255,0.14)' },
    ACCEPTEE: { label: 'Acceptée', color: '#5BBF73', bg: 'rgba(91,191,115,0.18)' },
    REFUSEE: { label: 'Refusée', color: '#ed8472', bg: 'rgba(229,96,77,0.13)' },
    ANNULEE: { label: 'Annulée', color: '#9a938c', bg: 'rgba(255,255,255,0.07)' },
  }

  function load() {
    const id = $currentBusinessId
    if (!id) return
    api(`/api/businesses/${id}/trades`).then((v) => (trades = v)).catch(fail)
  }
  $effect(() => {
    const id = $currentBusinessId
    if (!id) return
    api('/api/businesses/directory').then((v) => (directory = v.filter((b) => b.id !== id))).catch(fail)
    api('/api/catalog/items').then((v) => (items = v.filter((i) => !i.system))).catch(fail)
    load()
  })
  onMount(() => {
    const unsub = onRealtime('TRADE', load)
    return () => unsub?.()
  })

  let itemName = $derived(new Map(items.map((i) => [i.id, i.name])))

  function addLine() {
    if (!pickItem) return
    const q = Math.max(1, Math.round(Number(pickQty) || 1))
    const ex = draftLines.find((l) => l.itemId === pickItem)
    if (ex) draftLines = draftLines.map((l) => (l.itemId === pickItem ? { ...l, quantity: l.quantity + q } : l))
    else draftLines = [...draftLines, { itemId: pickItem, quantity: q }]
    pickQty = '1'
  }

  async function create() {
    if (!target) return notifyError('Choisis un business destinataire')
    if (draftLines.length === 0) return notifyError('Ajoute au moins une ligne')
    const n = Math.max(0, Math.round(Number(septims) || 0))
    try {
      await api(`/api/businesses/${$currentBusinessId}/trades`, {
        method: 'POST',
        body: JSON.stringify({ toBusinessId: target, septims: n, note: note || null, lines: draftLines }),
      })
      notifySuccess('Échange proposé')
      showCreate = false
      target = ''; septims = ''; note = ''; draftLines = []; pickItem = ''; pickQty = '1'
      load()
    } catch (e) {
      fail(e)
    }
  }

  async function act(id, action, confirmMsg) {
    if (confirmMsg && !confirm(confirmMsg)) return
    try {
      await api(`/api/businesses/${$currentBusinessId}/trades/${id}/${action}`, { method: 'POST' })
      notifySuccess(action === 'accept' ? 'Échange accepté — marchandise et septims transférés' : action === 'refuse' ? 'Échange refusé' : 'Échange annulé')
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<PageHeader title="Commerce" description="Échanges entre business — marchandise contre septims." />

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut).</p>
{:else}
  <div class="flex justify-end">
    {#if canOperate}
      <Button onclick={() => (showCreate = true)}><Plus size={16} /> Proposer un échange</Button>
    {/if}
  </div>

  <div class="mt-4 overflow-auto rounded-xl border">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b bg-muted/30 text-left text-xs uppercase tracking-wide text-muted-foreground">
          <th class="px-4 py-3">N°</th>
          <th class="px-4 py-3">Sens</th>
          <th class="px-4 py-3">Partenaire</th>
          <th class="px-4 py-3">Marchandise</th>
          <th class="px-4 py-3 text-right">Contrepartie</th>
          <th class="px-4 py-3 text-center">Statut</th>
          <th class="px-4 py-3">Date</th>
        </tr>
      </thead>
      <tbody>
        {#each trades as t (t.id)}
          {@const sent = t.fromBusinessId === $currentBusinessId}
          {@const st = STATUS[t.status] ?? STATUS.PROPOSEE}
          <tr onclick={() => (open = open === t.id ? null : t.id)} class="cursor-pointer border-b transition hover:bg-muted/20">
            <td class="px-4 py-3 font-medium tabular-nums">#{String(t.numero).padStart(4, '0')}</td>
            <td class="px-4 py-3">{sent ? '→ Envoyée' : '← Reçue'}</td>
            <td class="px-4 py-3">{sent ? t.toBusinessName : t.fromBusinessName}</td>
            <td class="max-w-[260px] truncate px-4 py-3 text-muted-foreground">{t.lines.map((l) => `${l.quantity}× ${l.itemName}`).join(', ')}</td>
            <td class="px-4 py-3 text-right font-semibold tabular-nums">{t.septims > 0 ? `${fmt(t.septims)} septims` : '— don'}</td>
            <td class="px-4 py-3 text-center"><span style="font-size:12px; font-weight:700; color:{st.color}; background:{st.bg}; padding:4px 10px; border-radius:999px;">{st.label}</span></td>
            <td class="px-4 py-3 text-muted-foreground">{dt(t.createdAt)}</td>
          </tr>
          {#if open === t.id}
            <tr class="border-b bg-muted/10">
              <td colspan="7" class="px-4 py-3">
                <div class="flex flex-wrap items-center justify-between gap-3 text-sm">
                  <div class="text-muted-foreground">
                    {t.fromBusinessName} → {t.toBusinessName}
                    {#if t.note}· {t.note}{/if}
                    {#if t.status === 'PROPOSEE' && !sent}
                      <span class="block text-xs">Accepter transfère la marchandise vers ton stock et sort {fmt(t.septims)} septims de ton coffre.</span>
                    {/if}
                  </div>
                  {#if canOperate && t.status === 'PROPOSEE'}
                    <div class="flex gap-2" role="presentation" onclick={(e) => e.stopPropagation()}>
                      {#if sent}
                        <Button variant="outline" onclick={() => act(t.id, 'cancel', 'Annuler cette proposition ?')}>Annuler</Button>
                      {:else}
                        <Button onclick={() => act(t.id, 'accept', `Accepter l'échange ? ${t.septims > 0 ? fmt(t.septims) + ' septims sortiront de ton coffre.' : ''}`)}>Accepter</Button>
                        <Button variant="outline" onclick={() => act(t.id, 'refuse', 'Refuser cet échange ?')}>Refuser</Button>
                      {/if}
                    </div>
                  {/if}
                </div>
              </td>
            </tr>
          {/if}
        {/each}
        {#if trades.length === 0}
          <tr><td colspan="7" class="px-4 py-6 text-center text-sm text-muted-foreground">Aucun échange.</td></tr>
        {/if}
      </tbody>
    </table>
  </div>

  <Modal bind:open={showCreate} title="Proposer un échange">
    <div class="flex flex-col gap-3">
      <label class="flex flex-col gap-1 text-xs text-muted-foreground">Business destinataire
        <SelectField value={target} onChange={(v) => (target = v)} options={directory.map((b) => ({ value: b.id, label: `${b.nom} (${b.type === 'COMPAGNIE' ? 'Compagnie' : 'Forge'})` }))} ariaLabel="Destinataire" placeholder="Choisir…" class="w-full" />
      </label>

      <div class="flex items-end gap-2 border-t pt-3">
        <label class="flex flex-1 flex-col gap-1 text-xs text-muted-foreground">Marchandise à envoyer
          <SelectField value={pickItem} onChange={(v) => (pickItem = v)} options={items.map((i) => ({ value: i.id, label: i.name }))} ariaLabel="Item" class="w-full" />
        </label>
        <label class="flex flex-col gap-1 text-xs text-muted-foreground">Qté
          <NumberInput value={pickQty} onchange={(v) => (pickQty = v)} min={1} class="w-24" />
        </label>
        <Button variant="outline" onclick={addLine}>Ajouter</Button>
      </div>

      {#if draftLines.length > 0}
        <div class="flex flex-col gap-1 rounded-lg border p-2">
          {#each draftLines as l (l.itemId)}
            <div class="flex items-center gap-2 text-sm">
              <span class="flex-1 truncate">{l.quantity}× {itemName.get(l.itemId) ?? '?'}</span>
              <button onclick={() => (draftLines = draftLines.filter((x) => x.itemId !== l.itemId))} aria-label="Retirer" class="text-muted-foreground hover:text-destructive"><Trash2 size={15} /></button>
            </div>
          {/each}
        </div>
      {/if}

      <div class="flex flex-wrap gap-3">
        <label class="flex flex-col gap-1 text-xs text-muted-foreground">Contrepartie demandée (septims)
          <NumberInput value={septims} onchange={(v) => (septims = v)} min={0} placeholder="0 = don" class="w-40" />
        </label>
        <label class="flex flex-1 flex-col gap-1 text-xs text-muted-foreground">Note (optionnel)
          <Input placeholder="ex. lot de fer de la semaine" bind:value={note} ariaLabel="Note" />
        </label>
      </div>

      <div class="flex justify-end gap-2">
        <Button variant="outline" onclick={() => (showCreate = false)}>Annuler</Button>
        <Button onclick={create}>Proposer</Button>
      </div>
    </div>
  </Modal>
{/if}
