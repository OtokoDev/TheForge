<script>
  import { ShoppingCart } from '@lucide/svelte'
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Fab from '../ui/Fab.svelte'

  let { businessId, canOperate, edit = null, onBack, onEmitted } = $props()
  const editId = edit?.id ?? null

  const ORANGE = '#E8590C', GREEN = '#5BBF73', TEXT = '#F4F1EE', MUTED = '#8f8880'
  const CARD = '#1c1a18', INPUT_BG = '#15110e', BORDER = '1px solid rgba(255,255,255,0.07)', DEFAULT_CAT = '#7d90a6'
  const fmt = (n) => Number(n).toLocaleString('fr-FR')
  const itemColor = (i) => i?.familyColor ?? DEFAULT_CAT
  const stepBtn = 'width:28px; height:28px; background:#232120; border:1px solid rgba(255,255,255,0.1); border-radius:7px; color:#cfc8c2; font-size:16px; cursor:pointer;'
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function dedupe(items, idKey, nameKey) {
    const m = new Map()
    for (const i of items) if (i[idKey] && i[nameKey]) m.set(i[idKey], i[nameKey])
    return Array.from(m, ([id, nom]) => ({ id, nom }))
  }

  let items = $state([])
  let prices = $state(new Map())
  let stockQty = $state(new Map())
  let recipes = $state(new Map()) // outputItemId → [{ componentItemId, quantity }]
  let query = $state('')
  let fam = $state('all')
  let mat = $state('all')
  let cart = $state(edit ? Object.fromEntries(edit.lines.map((l) => [l.itemId, l.quantity])) : {})
  let client = $state(edit?.clientName ?? '')
  let paid = $state(true)

  $effect(() => {
    api('/api/catalog/items').then((v) => (items = v)).catch(fail)
    api('/api/catalog/recipes')
      .then((edges) => {
        const m = new Map()
        for (const e of edges) {
          if (!m.has(e.outputItemId)) m.set(e.outputItemId, [])
          m.get(e.outputItemId).push({ componentItemId: e.componentItemId, quantity: e.quantity })
        }
        recipes = m
      })
      .catch(fail)
    api(`/api/businesses/${businessId}/products`)
      .then((rows) => (prices = new Map(rows.filter((r) => r.prixRevente != null).map((r) => [r.itemId, r.prixRevente]))))
      .catch(fail)
    api(`/api/businesses/${businessId}/stock`)
      .then((rows) => {
        const m = new Map()
        for (const r of rows) m.set(r.itemId, (m.get(r.itemId) ?? 0) + r.quantity)
        stockQty = m
      })
      .catch(fail)
  })

  // Disponible : en stock. Fabricable : pas en stock mais tous les ingrédients de la recette
  // sont en stock (forgé à la vente côté backend). Sinon indisponible. `rank` sert au tri.
  function stateOf(it) {
    if ((stockQty.get(it.id) ?? 0) > 0) return { label: 'Disponible', color: GREEN, bg: 'rgba(91,191,115,0.13)', sellable: true, rank: 0 }
    const recipe = recipes.get(it.id)
    if (recipe && recipe.length > 0 && recipe.every((c) => (stockQty.get(c.componentItemId) ?? 0) >= c.quantity))
      return { label: 'Fabricable', color: '#d9a441', bg: 'rgba(217,164,65,0.13)', sellable: true, rank: 1 }
    return { label: 'Indisponible', color: '#E5604D', bg: 'rgba(229,96,77,0.13)', sellable: false, rank: 2 }
  }

  let sellable = $derived(items.filter((i) => !i.system && prices.has(i.id)))
  let fams = $derived(dedupe(sellable, 'familyId', 'familyName'))
  let mats = $derived(dedupe(sellable, 'materialId', 'materialName'))
  let catalogue = $derived.by(() => {
    const q = query.trim().toLowerCase()
    return sellable
      .filter((i) => (fam === 'all' || i.familyId === fam) && (mat === 'all' || i.materialId === mat) && (q === '' || i.name.toLowerCase().includes(q)))
      .sort((a, b) => stateOf(a).rank - stateOf(b).rank || a.name.localeCompare(b.name))
  })
  let itemById = $derived(new Map(items.map((i) => [i.id, i])))
  let lines = $derived(Object.entries(cart).map(([id, qty]) => ({ id, item: itemById.get(id), qty })))
  let total = $derived(lines.reduce((s, l) => s + (prices.get(l.id) ?? 0) * l.qty, 0))
  let count = $derived(lines.reduce((s, l) => s + l.qty, 0))

  function add(id) {
    cart = { ...cart, [id]: (cart[id] ?? 0) + 1 }
  }
  function dec(id) {
    const n = { ...cart }
    n[id] = (n[id] ?? 0) - 1
    if (n[id] <= 0) delete n[id]
    cart = n
  }

  async function emit(asDraft) {
    if (Object.keys(cart).length === 0) return notifyError('Panier vide')
    const body = { lines: lines.map((l) => ({ itemId: l.id, quantity: l.qty })), clientName: client || null }
    try {
      let factureId = editId
      if (editId) {
        await api(`/api/businesses/${businessId}/factures/${editId}`, { method: 'PUT', body: JSON.stringify(body) })
      } else {
        const created = await api(`/api/businesses/${businessId}/factures`, { method: 'POST', body: JSON.stringify(body) })
        factureId = created.id
      }
      if (!asDraft) {
        await api(`/api/businesses/${businessId}/factures/${factureId}/validate`, { method: 'POST', body: JSON.stringify({ paid }) })
      }
      notifySuccess(asDraft ? (editId ? 'Brouillon mis à jour' : 'Brouillon enregistré') : 'Facture émise')
      onEmitted()
    } catch (e) {
      fail(e)
    }
  }

  const chip = (active) =>
    `background:${active ? ORANGE : '#1f1d1b'}; color:${active ? '#fff' : '#cfc8c2'}; border:${active ? `1px solid ${ORANGE}` : '1px solid rgba(255,255,255,0.08)'}; border-radius:8px; padding:5px 11px; font-size:12px; font-weight:600; cursor:pointer;`
</script>

{#snippet filterRow(labelText, current, pick, options)}
  {#if options.length > 0}
    <div style="display:flex; gap:6px; flex-wrap:wrap; align-items:center; margin-bottom:8px;">
      <span style="color:{MUTED}; font-size:11px; text-transform:uppercase; letter-spacing:.05em; width:64px;">{labelText}</span>
      <button onclick={() => pick('all')} style={chip(current === 'all')}>Tous</button>
      {#each options as o (o.id)}<button onclick={() => pick(o.id)} style={chip(current === o.id)}>{o.nom}</button>{/each}
    </div>
  {/if}
{/snippet}

<div>
  <div style="display:flex; align-items:center; gap:12px; margin-bottom:16px;">
    <button onclick={onBack} style="background:transparent; border:none; color:{MUTED}; font-size:13px; font-weight:600; cursor:pointer;">← Factures</button>
    <div style="color:{TEXT}; font-size:20px; font-weight:700;">{editId ? `Modifier le brouillon #${String(edit.numero).padStart(4, '0')}` : 'Nouvelle facture'}</div>
  </div>

  <div style="display:flex; gap:16px; align-items:flex-start; flex-wrap:wrap;">
    <div style="flex:1 1 340px; min-width:0;">
      <input bind:value={query} placeholder="Chercher un article du catalogue…" aria-label="Chercher un article" style="width:100%; background:{CARD}; border:1px solid rgba(255,255,255,0.1); border-radius:9px; color:{TEXT}; font-size:13.5px; padding:10px 12px; outline:none; margin-bottom:12px;" />
      {@render filterRow('Famille', fam, (v) => (fam = v), fams)}
      {@render filterRow('Matériau', mat, (v) => (mat = v), mats)}
      <div style="height:6px;"></div>
      <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px;">
        {#each catalogue as it (it.id)}
          {@const st = stateOf(it)}
          <button onclick={() => st.sellable && add(it.id)} disabled={!st.sellable}
            style="text-align:left; background:{CARD}; border:{BORDER}; border-radius:11px; padding:13px; cursor:{st.sellable ? 'pointer' : 'not-allowed'}; opacity:{st.sellable ? 1 : 0.5}; display:flex; flex-direction:column; gap:9px;">
            <div style="display:flex; align-items:center; gap:10px;">
              <div style="width:34px; height:34px; border-radius:9px; background:{itemColor(it)}; display:flex; align-items:center; justify-content:center; color:#16110d; font-weight:800; font-size:14px; flex:none;">{it.name.slice(0, 2).toUpperCase()}</div>
              <div style="min-width:0; line-height:1.25;">
                <div style="color:{TEXT}; font-weight:600; font-size:13.5px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">{it.name}</div>
                <div style="color:{MUTED}; font-size:11.5px;">{[it.familyName, it.materialName].filter(Boolean).join(' · ')}</div>
              </div>
            </div>
            <div style="display:flex; align-items:center; justify-content:space-between;">
              <span style="display:inline-flex; align-items:center; gap:6px; font-size:11.5px; font-weight:600; color:{st.color}; background:{st.bg}; padding:3px 8px; border-radius:6px;">
                <span style="width:6px; height:6px; border-radius:999px; background:{st.color};"></span>{st.label}
              </span>
              <span style="color:{TEXT}; font-weight:700; font-size:14px; font-variant-numeric:tabular-nums;">{fmt(prices.get(it.id) ?? 0)}</span>
            </div>
          </button>
        {/each}
      </div>
    </div>

    <div id="pos-cart" style="width:400px; flex:1 1 340px; max-width:100%; background:#171513; border:{BORDER}; border-radius:12px; display:flex; flex-direction:column;">
      <div style="padding:16px 18px 10px;">
        <div style="color:{MUTED}; font-size:11.5px; text-transform:uppercase; letter-spacing:.06em; font-weight:600; margin-bottom:8px;">Client</div>
        <input bind:value={client} placeholder="Client de passage (optionnel)" style="width:100%; background:{CARD}; border:1px solid rgba(255,255,255,0.1); border-radius:9px; color:{TEXT}; font-size:13.5px; padding:10px 12px; outline:none;" />
      </div>

      <div style="flex:1; min-height:120px; max-height:360px; overflow:auto; padding:4px 18px;">
        {#if lines.length === 0}
          <div style="color:#6f6862; font-size:13.5px; text-align:center; padding:40px 0;">Clique un article pour l'ajouter</div>
        {:else}
          {#each lines as l (l.id)}
            <div style="display:flex; align-items:center; gap:11px; padding:11px 0; border-bottom:1px solid rgba(255,255,255,0.06);">
              <div style="width:32px; height:32px; border-radius:8px; background:{itemColor(l.item)}; display:flex; align-items:center; justify-content:center; color:#16110d; font-weight:800; font-size:13px; flex:none;">{(l.item?.name ?? '?').slice(0, 2).toUpperCase()}</div>
              <div style="flex:1; min-width:0; line-height:1.3;">
                <div style="color:{TEXT}; font-weight:600; font-size:13.5px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">{l.item?.name}</div>
                <div style="color:{MUTED}; font-size:12px;">{fmt(prices.get(l.id) ?? 0)} / u</div>
              </div>
              <div style="display:flex; align-items:center; gap:5px; flex:none;">
                <button onclick={() => dec(l.id)} style={stepBtn}>−</button>
                <span style="min-width:24px; text-align:center; color:{TEXT}; font-weight:700;">{l.qty}</span>
                <button onclick={() => add(l.id)} style={stepBtn}>+</button>
              </div>
              <div style="width:74px; text-align:right; color:{TEXT}; font-weight:700; font-size:13.5px; font-variant-numeric:tabular-nums; flex:none;">{fmt((prices.get(l.id) ?? 0) * l.qty)}</div>
            </div>
          {/each}
        {/if}
      </div>

      <div style="border-top:1px solid rgba(255,255,255,0.08); padding:16px 18px; background:#19110d;">
        <div style="display:flex; justify-content:space-between; color:#9a938c; font-size:13px; margin-bottom:6px;"><span>Sous-total · {count} articles</span><span style="font-variant-numeric:tabular-nums;">{fmt(total)}</span></div>
        <div style="display:flex; justify-content:space-between; align-items:baseline; margin-bottom:14px;">
          <span style="color:{TEXT}; font-size:15px; font-weight:700;">Total</span>
          <span style="color:{ORANGE}; font-size:27px; font-weight:800; font-variant-numeric:tabular-nums;">{fmt(total)} <span style="font-size:13px; color:{MUTED}; font-weight:500;">septims</span></span>
        </div>
        <div style="display:flex; background:{INPUT_BG}; border:1px solid rgba(255,255,255,0.08); border-radius:8px; padding:3px; gap:3px; margin-bottom:12px;">
          <button onclick={() => (paid = true)} style="flex:1; background:{paid ? '#2f7a45' : 'transparent'}; color:{paid ? '#fff' : MUTED}; border:none; border-radius:6px; padding:8px; font-size:12.5px; font-weight:700; cursor:pointer;">Payé</button>
          <button onclick={() => (paid = false)} style="flex:1; background:{!paid ? '#9a4438' : 'transparent'}; color:{!paid ? '#fff' : MUTED}; border:none; border-radius:6px; padding:8px; font-size:12.5px; font-weight:700; cursor:pointer;">Non payé</button>
        </div>
        <button onclick={() => emit(false)} disabled={!canOperate}
          style="width:100%; background:{ORANGE}; border:none; color:#fff; font-size:15px; font-weight:700; padding:14px; border-radius:11px; cursor:{canOperate ? 'pointer' : 'not-allowed'}; opacity:{canOperate ? 1 : 0.5}; box-shadow:0 6px 18px rgba(232,89,12,0.32);">
          {paid ? `Émettre & encaisser · ${fmt(total)} septims` : `Émettre (non payé) · ${fmt(total)} septims`}
        </button>
        <button onclick={() => emit(true)} disabled={!canOperate} style="width:100%; margin-top:9px; background:transparent; border:1px solid rgba(255,255,255,0.12); color:#cfc8c2; font-size:13px; font-weight:600; padding:10px; border-radius:9px; cursor:pointer;">{editId ? 'Mettre à jour le brouillon' : 'Enregistrer comme brouillon'}</button>
      </div>
    </div>
  </div>

  {#if count > 0}
    <Fab icon={ShoppingCart} label="Voir le panier" badge={count} onclick={() => document.getElementById('pos-cart')?.scrollIntoView({ behavior: 'smooth' })} />
  {/if}
</div>
