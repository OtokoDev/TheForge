<script>
  import { me, currentBusinessId } from '../lib/session.js'
  import { canOperateBusiness } from '../lib/roles.js'
  import { api, ApiError } from '../lib/api.js'
  import { notifyError, notifySuccess } from '../lib/notifications.js'
  import Modal from '../components/ui/Modal.svelte'
  import Button from '../components/ui/Button.svelte'
  import Input from '../components/ui/Input.svelte'
  import SelectField from '../components/ui/SelectField.svelte'
  import NumberInput from '../components/ui/NumberInput.svelte'
  import { Trash2, Plus } from '@lucide/svelte'

  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  let canOperate = $derived($currentBusinessId ? canOperateBusiness($me, $currentBusinessId) : false)

  let purchases = $state([])
  let items = $state([])
  let open = $state(null)

  let showCreate = $state(false)
  let supplier = $state('')
  let draftLines = $state([]) // [{ itemId, quantity, unitCost }]
  let pickItem = $state('')
  let pickQty = $state('1')

  function load() {
    const id = $currentBusinessId
    if (!id) return
    api(`/api/businesses/${id}/purchases`).then((v) => (purchases = v)).catch(fail)
  }
  $effect(() => {
    const id = $currentBusinessId
    if (!id) return
    api('/api/catalog/items').then((v) => (items = v)).catch(fail)
    load()
  })

  let itemName = $derived(new Map(items.map((i) => [i.id, i.name])))
  const draftTotal = $derived(draftLines.reduce((s, l) => s + Number(l.unitCost) * l.quantity, 0))

  function addLine() {
    if (!pickItem) return
    const q = Math.max(1, Math.round(Number(pickQty) || 1))
    const ex = draftLines.find((l) => l.itemId === pickItem)
    if (ex) draftLines = draftLines.map((l) => (l.itemId === pickItem ? { ...l, quantity: l.quantity + q } : l))
    else draftLines = [...draftLines, { itemId: pickItem, quantity: q, unitCost: 0 }]
    pickQty = '1'
  }

  async function create() {
    if (draftLines.length === 0) return notifyError('Ajoute au moins une ligne')
    const body = {
      supplierName: supplier || null,
      lines: draftLines.map((l) => ({ itemId: l.itemId, quantity: l.quantity, unitPrice: Number(l.unitCost) })),
    }
    try {
      await api(`/api/businesses/${$currentBusinessId}/purchases`, { method: 'POST', body: JSON.stringify(body) })
      notifySuccess('Achat enregistré — stock et coffre mis à jour')
      showCreate = false
      supplier = ''; draftLines = []; pickItem = ''; pickQty = '1'
      load()
    } catch (e) {
      fail(e)
    }
  }

  const dt = (iso) => new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
</script>

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut).</p>
{:else}
  <div class="flex justify-end">
    {#if canOperate}
      <Button onclick={() => (showCreate = true)}><Plus size={16} /> Nouvel achat</Button>
    {/if}
  </div>

  <div class="mt-4 overflow-auto rounded-xl border">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b bg-muted/30 text-left text-xs uppercase tracking-wide text-muted-foreground">
          <th class="px-4 py-3">N°</th>
          <th class="px-4 py-3">Fournisseur</th>
          <th class="px-4 py-3">Matières</th>
          <th class="px-4 py-3">Date</th>
          <th class="px-4 py-3 text-right">Coût</th>
        </tr>
      </thead>
      <tbody>
        {#each purchases as p (p.id)}
          <tr onclick={() => (open = open === p.id ? null : p.id)} class="cursor-pointer border-b transition hover:bg-muted/20">
            <td class="px-4 py-3 font-medium tabular-nums">#{String(p.numero).padStart(4, '0')}</td>
            <td class="px-4 py-3">{p.supplierName ?? '—'}</td>
            <td class="max-w-[260px] truncate px-4 py-3 text-muted-foreground">{p.lines.map((l) => `${l.quantity}× ${l.itemName}`).join(', ')}</td>
            <td class="px-4 py-3 text-muted-foreground">{dt(p.createdAt)}</td>
            <td class="px-4 py-3 text-right font-semibold tabular-nums">{fmt(p.total)}</td>
          </tr>
          {#if open === p.id}
            <tr class="border-b bg-muted/10">
              <td colspan="5" class="px-4 py-3 text-sm text-muted-foreground">
                {p.lines.map((l) => `${l.quantity}× ${l.itemName} (${fmt(l.lineTotal)})`).join(' · ')}
              </td>
            </tr>
          {/if}
        {/each}
        {#if purchases.length === 0}
          <tr><td colspan="5" class="px-4 py-6 text-center text-sm text-muted-foreground">Aucun achat.</td></tr>
        {/if}
      </tbody>
    </table>
  </div>

  <Modal bind:open={showCreate} title="Nouvel achat">
    <div class="flex flex-col gap-3">
      <Input placeholder="Fournisseur (optionnel)" bind:value={supplier} ariaLabel="Fournisseur" />
      <div class="flex items-end gap-2 border-t pt-3">
        <label class="flex flex-1 flex-col gap-1 text-xs text-muted-foreground">Matière
          <SelectField value={pickItem} onChange={(v) => (pickItem = v)} options={items.map((i) => ({ value: i.id, label: i.name }))} ariaLabel="Matière" />
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
              <NumberInput value={String(l.unitCost)} onchange={(v) => (draftLines = draftLines.map((x) => (x.itemId === l.itemId ? { ...x, unitCost: Number(v) } : x)))} min={0} class="w-24" />
              <span class="text-xs text-muted-foreground">coût/u</span>
              <button onclick={() => (draftLines = draftLines.filter((x) => x.itemId !== l.itemId))} aria-label="Retirer" class="text-muted-foreground hover:text-destructive"><Trash2 size={15} /></button>
            </div>
          {/each}
          <div class="mt-1 border-t pt-1 text-right text-sm font-semibold">Coût total : {fmt(draftTotal)} septims</div>
        </div>
      {/if}

      <div class="flex justify-end gap-2">
        <Button variant="outline" onclick={() => (showCreate = false)}>Annuler</Button>
        <Button onclick={create}>Enregistrer l'achat</Button>
      </div>
    </div>
  </Modal>
{/if}
