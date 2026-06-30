<script>
  import { onMount } from 'svelte'
  import { me, currentBusinessId } from '../lib/session.js'
  import { api, ApiError } from '../lib/api.js'
  import { onRealtime } from '../lib/realtime.js'
  import { canAdminBusiness, canOperateBusiness } from '../lib/roles.js'
  import { notifyError, notifySuccess } from '../lib/notifications.js'
  import NumberInput from '../components/ui/NumberInput.svelte'
  import Fab from '../components/ui/Fab.svelte'

  const ORANGE = '#E8590C', TEXT = '#F4F1EE', MUTED = '#8f8880'
  const CARD = '#1c1a18', TABLE_BG = '#1a1816', HEAD_BG = '#221f1b', INPUT_BG = '#15110e'
  const BORDER = '1px solid rgba(255,255,255,0.07)', DEFAULT_CAT = '#7d90a6'
  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const initials = (n) => n.slice(0, 2).toUpperCase()
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  const thStyle = `color:${MUTED}; font-weight:600; font-size:12px; letter-spacing:.03em; padding:13px 16px; cursor:pointer; border-bottom:${BORDER}; white-space:nowrap;`
  const tdNum = `padding:11px 16px; text-align:right; color:${TEXT}; font-variant-numeric:tabular-nums;`
  const quickBtn = 'flex:1; background:#232120; border:1px solid rgba(255,255,255,0.08); border-radius:7px; color:#9a938c; font-size:12.5px; padding:6px; cursor:pointer;'
  const stepBtn = 'width:26px; height:26px; background:#232120; border:1px solid rgba(255,255,255,0.1); border-radius:6px; color:#cfc8c2; font-size:15px; cursor:pointer; flex:none;'
  const pickerStyle = `width:100%; background:${INPUT_BG}; border:1px solid rgba(255,255,255,0.12); border-radius:9px; color:${TEXT}; font-size:14px; font-weight:600; padding:11px 13px; outline:none; cursor:pointer;`
  const chipStyle = (active) =>
    `display:flex; align-items:center; gap:7px; background:${active ? 'rgba(232,89,12,0.15)' : 'rgba(255,255,255,0.05)'}; color:${active ? '#f5a06a' : '#cfc8c2'}; border:none; border-radius:999px; padding:7px 13px; font-size:13px; font-weight:600; cursor:pointer;`

  let canAdmin = $derived($currentBusinessId ? canAdminBusiness($me, $currentBusinessId) : false)
  let canOperate = $derived($currentBusinessId ? canOperateBusiness($me, $currentBusinessId) : false)

  let accounts = $state([])
  let items = $state([])
  let prices = $state(new Map())
  let stock = $state([])
  let movements = $state([])

  let query = $state('')
  let fam = $state('all')
  let mat = $state('all')
  let sort = $state({ key: 'name', dir: 'asc' })
  let inventory = $state(false)
  let counted = $state({})

  let sel = $state(null)
  let mode = $state('DEPOSIT')
  let qty = $state('')
  let motif = $state('')
  let transferTo = $state('')
  let formItem = $state('')
  let formAccount = $state('')

  // Panier de dépôt — déposer plusieurs objets en une fois (POST /movements/batch).
  let depositOpen = $state(false)
  let depositAccount = $state('')
  let depositCart = $state({})
  let depositQuery = $state('')

  function distinct(rows, idKey, nameKey) {
    const m = new Map()
    for (const r of rows) {
      const id = r[idKey], nom = r[nameKey]
      if (id && nom) m.set(id, { nom, count: (m.get(id)?.count ?? 0) + 1 })
    }
    return Array.from(m, ([id, v]) => ({ id, nom: v.nom, count: v.count }))
  }

  function loadStock() {
    const id = $currentBusinessId
    if (!id) return
    api(`/api/businesses/${id}/stock`).then((v) => (stock = v)).catch(fail)
    api(`/api/businesses/${id}/movements`).then((v) => (movements = v)).catch(fail)
  }

  $effect(() => {
    const id = $currentBusinessId
    if (!id) return
    api(`/api/businesses/${id}/accounts`).then((v) => (accounts = v)).catch(fail)
    api('/api/catalog/items').then((v) => (items = v)).catch(fail)
    api(`/api/businesses/${id}/costs`).then((cs) => (prices = new Map(cs.map((c) => [c.itemId, c.cost])))).catch(fail)
    loadStock()
  })

  onMount(() => onRealtime('STOCK', loadStock))

  let itemById = $derived(new Map(items.map((i) => [i.id, i])))
  let enriched = $derived.by(() =>
    stock.map((r) => {
      const it = itemById.get(r.itemId)
      const unit = prices.get(r.itemId) ?? 0
      return {
        ...r,
        familyId: it?.familyId ?? null, familyName: it?.familyName ?? null,
        materialId: it?.materialId ?? null, materialName: it?.materialName ?? null,
        color: it?.familyColor ?? DEFAULT_CAT, unit, total: r.quantity * unit,
      }
    }),
  )
  let famOpts = $derived(distinct(enriched, 'familyId', 'familyName'))
  let matOpts = $derived(distinct(enriched, 'materialId', 'materialName'))
  let rows = $derived.by(() => {
    const q = query.trim().toLowerCase()
    const filtered = enriched.filter((r) => (fam === 'all' || r.familyId === fam) && (mat === 'all' || r.materialId === mat) && (q === '' || r.itemName.toLowerCase().includes(q)))
    const dir = sort.dir === 'asc' ? 1 : -1
    return [...filtered].sort((a, b) => {
      switch (sort.key) {
        case 'chest': return a.accountName.localeCompare(b.accountName) * dir
        case 'qty': return (a.quantity - b.quantity) * dir
        case 'unit': return (a.unit - b.unit) * dir
        case 'total': return (a.total - b.total) * dir
        default: return a.itemName.localeCompare(b.itemName) * dir
      }
    })
  })
  let kpis = $derived.by(() => ({
    value: enriched.reduce((s, r) => s + r.total, 0),
    qty: enriched.reduce((s, r) => s + r.quantity, 0),
    refs: new Set(enriched.map((r) => r.itemId)).size,
    chests: accounts.length,
  }))
  let ecartCount = $derived.by(() => {
    let n = 0
    for (const r of rows) {
      const v = counted[`${r.accountId}|${r.itemId}`]
      if (v !== undefined && v !== '' && Number(v) - r.quantity !== 0) n++
    }
    return n
  })

  let selItemId = $derived(sel ? sel.itemId || formItem : '')
  let selAccountId = $derived(sel ? sel.accountId || formAccount : '')
  let selRow = $derived(sel?.itemId ? enriched.find((r) => r.itemId === sel.itemId && r.accountId === sel.accountId) : undefined)
  let selItem = $derived(itemById.get(selItemId))

  const arrow = (key) => (sort.key === key ? (sort.dir === 'asc' ? '▲' : '▼') : '')
  function toggleSort(key) {
    sort = sort.key === key ? { key, dir: sort.dir === 'asc' ? 'desc' : 'asc' } : { key, dir: 'asc' }
  }
  function openRow(r) {
    if (inventory) return
    sel = { itemId: r.itemId, accountId: r.accountId }
    mode = 'DEPOSIT'; qty = ''; motif = ''; transferTo = ''
  }
  function setCount(key, v) {
    counted = { ...counted, [key]: v }
  }

  async function submitMove() {
    if (!$currentBusinessId || !selItemId || !selAccountId) return notifyError('Item et compte requis')
    const n = Number(qty)
    if (!qty || n <= 0) return notifyError('Quantité (> 0) requise')
    let fromAccountId = mode === 'DEPOSIT' ? null : selAccountId
    let toAccountId = mode === 'DEPOSIT' ? selAccountId : null
    let type = mode === 'DEPOSIT' ? 'DEPOSIT' : 'WITHDRAWAL'
    if (mode === 'TRANSFER') {
      if (!transferTo) return notifyError('Choisis le coffre destination')
      toAccountId = transferTo
      type = 'TRANSFER'
    }
    try {
      await api(`/api/businesses/${$currentBusinessId}/movements`, {
        method: 'POST',
        body: JSON.stringify({ itemId: selItemId, quantity: n, fromAccountId, toAccountId, type, note: motif || null }),
      })
      notifySuccess('Mouvement enregistré')
      qty = ''; motif = ''
      loadStock()
    } catch (e) {
      fail(e)
    }
  }

  let depositLines = $derived(Object.entries(depositCart).map(([id, qty]) => ({ id, name: itemById.get(id)?.name ?? '?', qty })))
  let depositTotal = $derived(depositLines.reduce((s, l) => s + l.qty, 0))
  let depositCatalog = $derived.by(() => {
    const q = depositQuery.trim().toLowerCase()
    return items.filter((i) => !i.system && (q === '' || i.name.toLowerCase().includes(q))).slice(0, 40)
  })

  function openDepositCart() {
    depositOpen = true
    depositAccount = accounts[0]?.id ?? ''
    depositCart = {}
    depositQuery = ''
  }
  function addToDeposit(id) {
    depositCart = { ...depositCart, [id]: (depositCart[id] ?? 0) + 1 }
  }
  function decDeposit(id) {
    const n = { ...depositCart }
    n[id] = (n[id] ?? 0) - 1
    if (n[id] <= 0) delete n[id]
    depositCart = n
  }
  function setDepositQty(id, v) {
    const q = Math.max(0, Math.floor(Number(v) || 0))
    const n = { ...depositCart }
    if (q <= 0) delete n[id]
    else n[id] = q
    depositCart = n
  }
  async function submitDeposit() {
    if (!$currentBusinessId) return
    if (!depositAccount) return notifyError('Choisis le coffre destination')
    if (depositLines.length === 0) return notifyError('Ajoute au moins un objet')
    const moves = depositLines.map((l) => ({ itemId: l.id, quantity: l.qty, fromAccountId: null, toAccountId: depositAccount, type: 'DEPOSIT', note: null }))
    try {
      const res = await api(`/api/businesses/${$currentBusinessId}/movements/batch`, { method: 'POST', body: JSON.stringify({ moves }) })
      notifySuccess(`${res.count} dépôt(s) enregistré(s)`)
      depositOpen = false
      loadStock()
    } catch (e) {
      fail(e)
    }
  }

  async function validateInventory() {
    if (!$currentBusinessId) return
    const payload = rows
      .map((r) => ({ key: `${r.accountId}|${r.itemId}`, r }))
      .filter(({ key }) => counted[key] !== undefined && counted[key] !== '')
      .map(({ key, r }) => ({ accountId: r.accountId, itemId: r.itemId, counted: Number(counted[key]) }))
    if (payload.length === 0) return notifyError('Aucune quantité comptée')
    try {
      const res = await api(`/api/businesses/${$currentBusinessId}/inventory`, { method: 'POST', body: JSON.stringify({ counts: payload }) })
      notifySuccess(`Inventaire validé — ${res.adjusted} ligne(s) régularisée(s)`)
      counted = {}
      inventory = false
      loadStock()
    } catch (e) {
      fail(e)
    }
  }
</script>

{#snippet filterRow(label, value, pick, options)}
  {#if options.length > 0}
    <div style="display:flex; gap:6px; flex-wrap:wrap; align-items:center;">
      <span style="color:{MUTED}; font-size:11px; text-transform:uppercase; letter-spacing:.05em; width:64px;">{label}</span>
      <button onclick={() => pick('all')} style={chipStyle(value === 'all')}>Tous</button>
      {#each options as o (o.id)}
        <button onclick={() => pick(o.id)} style={chipStyle(value === o.id)}>
          {o.nom}<span style="font-size:11px; font-weight:700; background:rgba(255,255,255,0.08); color:{MUTED}; padding:2px 6px; border-radius:5px;">{o.count}</span>
        </button>
      {/each}
    </div>
  {/if}
{/snippet}

{#snippet picker(value, onChange, options)}
  <select {value} onchange={(e) => onChange(e.currentTarget.value)} style="{pickerStyle} color-scheme:dark;">
    {#each options as o (o.value)}<option value={o.value}>{o.label}</option>{/each}
  </select>
{/snippet}

{#if !$currentBusinessId}
  <p style="color:{MUTED}; font-size:14px;">Sélectionne un business (en haut) pour gérer le stock.</p>
{:else}
  <div>
    <div style="display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:15px;">
      <div>
        <div style="color:{TEXT}; font-size:24px; font-weight:700;">Stock</div>
        <div style="color:{MUTED}; font-size:13.5px; margin-top:3px;">Inventaire commun · tous les coffres</div>
      </div>
      <div style="display:flex; gap:10px;">
        {#if canAdmin}
          <button onclick={() => { inventory = !inventory; sel = null }}
            style="display:flex; align-items:center; gap:8px; background:{inventory ? 'rgba(232,89,12,0.15)' : 'transparent'}; border:1px solid {inventory ? 'rgba(232,89,12,0.4)' : 'rgba(255,255,255,0.13)'}; color:{inventory ? '#f5a06a' : '#cfc8c2'}; font-size:13.5px; font-weight:600; padding:10px 14px; border-radius:9px; cursor:pointer;">
            Inventaire <span style="font-size:10px; font-weight:700; background:rgba(255,255,255,0.08); color:{MUTED}; padding:2px 5px; border-radius:4px; letter-spacing:.05em;">ADMIN</span>
          </button>
        {/if}
        {#if canOperate && !inventory}
          <button onclick={openDepositCart} style="display:flex; align-items:center; gap:8px; background:{ORANGE}; border:none; color:#fff; font-size:13.5px; font-weight:700; padding:10px 17px; border-radius:9px; cursor:pointer; box-shadow:0 4px 14px rgba(232,89,12,0.32);">+ Déposer</button>
        {/if}
      </div>
    </div>

    <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:12px; margin-bottom:15px;">
      {#each [{ l: 'Valeur du stock', v: `${fmt(kpis.value)} septims` }, { l: 'Objets en stock', v: fmt(kpis.qty) }, { l: 'Références', v: fmt(kpis.refs) }, { l: 'Coffres', v: fmt(kpis.chests) }] as k (k.l)}
        <div style="background:{CARD}; border:{BORDER}; border-radius:12px; padding:15px 17px;">
          <div style="color:{MUTED}; font-size:11.5px; text-transform:uppercase; letter-spacing:.06em; font-weight:600;">{k.l}</div>
          <div style="color:{TEXT}; font-size:25px; font-weight:700; margin-top:6px;">{k.v}</div>
        </div>
      {/each}
    </div>

    {#if inventory}
      <div style="display:flex; align-items:center; justify-content:space-between; background:rgba(232,89,12,0.1); border:1px solid rgba(232,89,12,0.32); border-radius:11px; padding:12px 16px; margin-bottom:15px;">
        <div style="color:#f0d8c4; font-size:13.5px;"><b style="color:{TEXT};">Mode inventaire</b> — saisis les quantités comptées en jeu. <span style="color:#f5a06a; font-weight:700;">{ecartCount} écart(s)</span> détecté(s).</div>
        <button onclick={validateInventory} style="background:{ORANGE}; border:none; color:#fff; font-size:13px; font-weight:700; padding:9px 15px; border-radius:8px; cursor:pointer;">Valider l'inventaire</button>
      </div>
    {/if}

    <div style="display:flex; flex-direction:column; gap:8px; margin-bottom:15px;">
      <input bind:value={query} placeholder="Rechercher un objet…" aria-label="Rechercher un objet" style="background:{CARD}; border:1px solid rgba(255,255,255,0.1); border-radius:9px; color:{TEXT}; font-size:13.5px; padding:9px 12px; width:240px; outline:none;" />
      {@render filterRow('Famille', fam, (v) => (fam = v), famOpts)}
      {@render filterRow('Matériau', mat, (v) => (mat = v), matOpts)}
    </div>

    <div style="overflow:auto; border:{BORDER}; border-radius:12px; background:{TABLE_BG};">
      <table style="width:100%; border-collapse:collapse; font-size:13.5px;">
        <thead>
          <tr style="background:{HEAD_BG};">
            <th onclick={() => toggleSort('name')} style="{thStyle} text-align:left;">OBJET <span style="color:{ORANGE}; font-size:10px;">{arrow('name')}</span></th>
            <th onclick={() => toggleSort('chest')} style="{thStyle} text-align:left;">COFFRE <span style="color:{ORANGE}; font-size:10px;">{arrow('chest')}</span></th>
            <th onclick={() => toggleSort('qty')} style="{thStyle} text-align:right;">{inventory ? 'SYSTÈME' : 'QUANTITÉ'} <span style="color:{ORANGE}; font-size:10px;">{arrow('qty')}</span></th>
            {#if inventory}<th style="{thStyle} text-align:right; color:{ORANGE}; cursor:default;">COMPTÉ</th>{/if}
            {#if inventory}<th style="{thStyle} text-align:right; cursor:default;">ÉCART</th>{/if}
            <th onclick={() => toggleSort('unit')} style="{thStyle} text-align:right;">VAL. UNIT. <span style="color:{ORANGE}; font-size:10px;">{arrow('unit')}</span></th>
            <th onclick={() => toggleSort('total')} style="{thStyle} text-align:right;">VAL. TOTALE <span style="color:{ORANGE}; font-size:10px;">{arrow('total')}</span></th>
          </tr>
        </thead>
        <tbody>
          {#each rows as r (`${r.accountId}|${r.itemId}`)}
            {@const key = `${r.accountId}|${r.itemId}`}
            {@const cVal = counted[key]}
            {@const ecart = cVal !== undefined && cVal !== '' ? Number(cVal) - r.quantity : 0}
            {@const selected = sel?.itemId === r.itemId && sel?.accountId === r.accountId}
            <tr onclick={() => openRow(r)} style="border-bottom:1px solid rgba(255,255,255,0.05); cursor:{inventory ? 'default' : 'pointer'}; background:{selected ? 'rgba(232,89,12,0.08)' : 'transparent'};">
              <td style="padding:11px 16px;">
                <div style="display:flex; align-items:center; gap:11px;">
                  <div style="width:31px; height:31px; border-radius:8px; background:{r.color}; display:flex; align-items:center; justify-content:center; color:#16110d; font-weight:800; font-size:13px; flex:none;">{initials(r.itemName)}</div>
                  <div style="line-height:1.25;">
                    <div style="color:{TEXT}; font-weight:600;">{r.itemName}</div>
                    <div style="color:{MUTED}; font-size:12px;">{[r.familyName, r.materialName].filter(Boolean).join(' · ') || '—'}</div>
                  </div>
                </div>
              </td>
              <td style="padding:11px 16px;"><span style="background:rgba(255,255,255,0.06); color:#cfc8c2; padding:4px 9px; border-radius:6px; font-size:12.5px;">{r.accountName}</span></td>
              <td style="{tdNum}{inventory ? ' color:#9a938c;' : ''}">{fmt(r.quantity)}</td>
              {#if inventory}
                <td style="padding:6px 14px; text-align:right;" onclick={(e) => e.stopPropagation()}>
                  <NumberInput variant="dark" class="w-24" value={cVal ?? ''} onchange={(v) => setCount(key, v)} />
                </td>
              {/if}
              {#if inventory}
                <td style="padding:8px 14px; text-align:right;">
                  <span style="display:inline-block; min-width:42px; background:{ecart === 0 ? 'rgba(255,255,255,0.06)' : ecart > 0 ? 'rgba(120,180,120,0.18)' : 'rgba(232,89,12,0.18)'}; color:{ecart === 0 ? MUTED : ecart > 0 ? '#88c088' : '#f5a06a'}; font-weight:700; font-size:12.5px; padding:3px 9px; border-radius:6px; font-variant-numeric:tabular-nums;">{cVal ? (ecart > 0 ? `+${ecart}` : ecart) : '—'}</span>
                </td>
              {/if}
              <td style="{tdNum} color:#9a938c;">{fmt(r.unit)}</td>
              <td style="{tdNum} font-weight:700;">{fmt(r.total)}</td>
            </tr>
          {/each}
        </tbody>
      </table>
      <div style="padding:11px 16px; color:#6f6862; font-size:12.5px; border-top:1px solid rgba(255,255,255,0.05);">{rows.length} référence(s) affichée(s)</div>
    </div>

    {#if sel && !inventory}
      <div style="position:fixed; top:0; right:0; bottom:0; width:min(386px,100vw); background:#1a1613; border-left:1px solid rgba(255,255,255,0.1); box-shadow:-26px 0 55px rgba(0,0,0,0.42); display:flex; flex-direction:column; z-index:50;">
        <div style="display:flex; align-items:center; justify-content:space-between; padding:16px 18px; border-bottom:{BORDER};">
          <div style="color:{MUTED}; font-size:11.5px; text-transform:uppercase; letter-spacing:.08em; font-weight:600;">Mouvement de stock</div>
          <button onclick={() => (sel = null)} aria-label="Fermer" style="background:transparent; border:none; color:{MUTED}; cursor:pointer; font-size:18px;">✕</button>
        </div>

        <div style="padding:18px; display:flex; flex-direction:column; gap:14px; overflow:auto;">
          {#if sel.itemId}
            <div style="display:flex; gap:13px; align-items:center;">
              <div style="width:46px; height:46px; border-radius:11px; background:{selItem?.familyColor ?? DEFAULT_CAT}; display:flex; align-items:center; justify-content:center; color:#16110d; font-weight:800; font-size:19px; flex:none;">{initials(selRow?.itemName ?? '?')}</div>
              <div style="line-height:1.3;">
                <div style="color:{TEXT}; font-size:17px; font-weight:700;">{selRow?.itemName}</div>
                <div style="color:{MUTED}; font-size:12.5px;">{[selItem?.familyName, selItem?.materialName].filter(Boolean).join(' · ') || '—'} · {selRow?.accountName}</div>
              </div>
            </div>
          {:else}
            <label style="display:block;"><div style="color:{MUTED}; font-size:11.5px; text-transform:uppercase; letter-spacing:.06em; font-weight:600; margin-bottom:8px;">Objet</div>{@render picker(formItem, (v) => (formItem = v), items.map((i) => ({ value: i.id, label: i.name })))}</label>
            <label style="display:block;"><div style="color:{MUTED}; font-size:11.5px; text-transform:uppercase; letter-spacing:.06em; font-weight:600; margin-bottom:8px;">Coffre</div>{@render picker(formAccount, (v) => (formAccount = v), accounts.map((a) => ({ value: a.id, label: a.name })))}</label>
          {/if}

          {#if selRow}
            <div style="display:flex; gap:9px;">
              {#each [{ l: 'Quantité', v: fmt(selRow.quantity), c: TEXT }, { l: 'Val. unit.', v: fmt(selRow.unit), c: TEXT }, { l: 'Total', v: fmt(selRow.total), c: ORANGE }] as s (s.l)}
                <div style="flex:1; background:{INPUT_BG}; border:{BORDER}; border-radius:9px; padding:10px 12px;">
                  <div style="color:{MUTED}; font-size:11px;">{s.l}</div>
                  <div style="color:{s.c}; font-size:17px; font-weight:700; margin-top:2px;">{s.v}</div>
                </div>
              {/each}
            </div>
          {/if}

          <div style="display:flex; background:{INPUT_BG}; border:1px solid rgba(255,255,255,0.08); border-radius:10px; padding:4px; gap:4px;">
            {#each [{ m: 'DEPOSIT', l: 'Déposer' }, { m: 'WITHDRAW', l: 'Retirer' }, { m: 'TRANSFER', l: 'Transférer' }] as opt (opt.m)}
              <button onclick={() => (mode = opt.m)} style="flex:1; background:{mode === opt.m ? ORANGE : 'transparent'}; color:{mode === opt.m ? '#fff' : '#cfc8c2'}; border:none; border-radius:7px; padding:9px; font-size:13px; font-weight:700; cursor:pointer;">{opt.l}</button>
            {/each}
          </div>

          {#if mode === 'TRANSFER'}
            <label style="display:block;"><div style="color:{MUTED}; font-size:11.5px; text-transform:uppercase; letter-spacing:.06em; font-weight:600; margin-bottom:8px;">Vers le coffre</div>{@render picker(transferTo, (v) => (transferTo = v), [{ value: '', label: '— choisir —' }, ...accounts.filter((a) => a.id !== selAccountId).map((a) => ({ value: a.id, label: a.name }))])}</label>
          {/if}

          <label style="display:block;">
            <div style="color:{MUTED}; font-size:11.5px; text-transform:uppercase; letter-spacing:.06em; font-weight:600; margin-bottom:8px;">Quantité</div>
            <NumberInput variant="dark" class="w-full" value={qty} onchange={(v) => (qty = v)} min={1} />
            <div style="display:flex; gap:7px; margin-top:9px;">
              {#each [1, 10, 100] as d (d)}<button onclick={() => (qty = String((Number(qty) || 0) + d))} style={quickBtn}>+{d}</button>{/each}
              {#if selRow}<button onclick={() => (qty = String(selRow.quantity))} style={quickBtn}>Max</button>{/if}
            </div>
          </label>

          <label style="display:block;">
            <div style="color:{MUTED}; font-size:11.5px; text-transform:uppercase; letter-spacing:.06em; font-weight:600; margin-bottom:8px;">Motif (optionnel)</div>
            <input bind:value={motif} placeholder="Ex. forge d'une commande…" style="width:100%; background:{INPUT_BG}; border:1px solid rgba(255,255,255,0.1); border-radius:9px; color:{TEXT}; font-size:13px; padding:9px 12px; outline:none;" />
          </label>

          <button onclick={submitMove} disabled={!canOperate} style="width:100%; background:{ORANGE}; border:none; color:#fff; font-size:14px; font-weight:700; padding:12px; border-radius:10px; cursor:{canOperate ? 'pointer' : 'not-allowed'}; opacity:{canOperate ? 1 : 0.5}; box-shadow:0 5px 16px rgba(232,89,12,0.28);">
            {mode === 'DEPOSIT' ? 'Déposer' : mode === 'WITHDRAW' ? 'Retirer' : 'Transférer'}
          </button>

          {#if sel.itemId}
            <div style="border-top:{BORDER}; padding-top:14px;">
              <div style="color:{MUTED}; font-size:11.5px; text-transform:uppercase; letter-spacing:.06em; font-weight:600; margin-bottom:10px;">Derniers mouvements</div>
              {#each movements.filter((m) => m.itemId === sel.itemId).slice(0, 6) as m (m.id)}
                <div style="display:flex; align-items:center; gap:11px; padding:9px 0; border-bottom:1px solid rgba(255,255,255,0.05);">
                  <span style="font-size:10.5px; font-weight:700; color:{ORANGE}; border:1px solid {ORANGE}; padding:2px 7px; border-radius:5px; flex:none;">{m.type}</span>
                  <div style="flex:1; min-width:0;">
                    <div style="color:#cfc8c2; font-size:12.5px;">{m.fromAccountName ?? 'création'} → {m.toAccountName ?? 'sortie'}</div>
                    <div style="color:#6f6862; font-size:11.5px;">{new Date(m.createdAt).toLocaleString('fr-FR')}</div>
                  </div>
                  <span style="color:{TEXT}; font-weight:700; font-size:13.5px;">{m.quantity}</span>
                </div>
              {/each}
            </div>
          {/if}
        </div>
      </div>
    {/if}

    {#if depositOpen}
      <div style="position:fixed; inset:0; z-index:50;">
        <div role="presentation" onclick={() => (depositOpen = false)} style="position:absolute; inset:0; background:rgba(0,0,0,0.5);"></div>
        <aside style="position:fixed; top:0; right:0; bottom:0; width:min(440px,100vw); background:#1a1613; border-left:1px solid rgba(255,255,255,0.1); box-shadow:-26px 0 55px rgba(0,0,0,0.42); display:flex; flex-direction:column; z-index:51;">
          <div style="display:flex; align-items:center; justify-content:space-between; padding:16px 18px; border-bottom:{BORDER};">
            <div style="color:{TEXT}; font-size:16px; font-weight:700;">Déposer des objets</div>
            <button onclick={() => (depositOpen = false)} aria-label="Fermer" style="background:transparent; border:none; color:{MUTED}; font-size:22px; cursor:pointer; line-height:1;">×</button>
          </div>
          <div style="padding:16px 18px; display:flex; flex-direction:column; gap:12px; overflow:auto; flex:1;">
            <div>
              <div style="color:{MUTED}; font-size:11px; text-transform:uppercase; letter-spacing:.05em; margin-bottom:6px;">Coffre destination</div>
              <select bind:value={depositAccount} aria-label="Coffre destination" style={pickerStyle}>
                {#each accounts as a (a.id)}<option value={a.id}>{a.name}</option>{/each}
              </select>
            </div>
            <input bind:value={depositQuery} placeholder="Chercher un objet à ajouter…" aria-label="Chercher un objet" style="width:100%; background:{INPUT_BG}; border:1px solid rgba(255,255,255,0.12); border-radius:9px; color:{TEXT}; font-size:13.5px; padding:9px 12px; outline:none;" />
            {#if depositQuery.trim()}
              <div style="display:flex; flex-direction:column; gap:4px; max-height:160px; overflow:auto;">
                {#each depositCatalog as it (it.id)}
                  <button onclick={() => addToDeposit(it.id)} style="display:flex; align-items:center; justify-content:space-between; background:{CARD}; border:{BORDER}; border-radius:8px; padding:8px 11px; color:{TEXT}; font-size:13px; cursor:pointer; text-align:left;">
                    <span>{it.name}</span><span style="color:{ORANGE}; font-weight:600;">+ ajouter</span>
                  </button>
                {/each}
              </div>
            {/if}
            <div style="border-top:{BORDER}; padding-top:10px; display:flex; flex-direction:column; gap:8px;">
              {#if depositLines.length === 0}
                <div style="color:#6f6862; font-size:13px; text-align:center; padding:18px 0;">Aucun objet — cherche et ajoute ci-dessus.</div>
              {:else}
                {#each depositLines as l (l.id)}
                  <div style="display:flex; align-items:center; gap:10px;">
                    <span style="flex:1; min-width:0; color:{TEXT}; font-size:13.5px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">{l.name}</span>
                    <button onclick={() => decDeposit(l.id)} style={stepBtn}>−</button>
                    <input type="number" min="1" step="1" value={l.qty} onchange={(e) => setDepositQty(l.id, e.currentTarget.value)} aria-label="Quantité" style="width:54px; text-align:center; background:{INPUT_BG}; border:1px solid rgba(255,255,255,0.1); border-radius:6px; color:{TEXT}; font-size:13px; padding:4px; outline:none;" />
                    <button onclick={() => addToDeposit(l.id)} style={stepBtn}>+</button>
                  </div>
                {/each}
              {/if}
            </div>
          </div>
          <div style="border-top:{BORDER}; padding:14px 18px; display:flex; align-items:center; justify-content:space-between; gap:12px;">
            <span style="color:{MUTED}; font-size:13px;">{depositTotal} objet(s)</span>
            <button onclick={submitDeposit} disabled={depositLines.length === 0} style="background:{ORANGE}; border:none; color:#fff; font-size:14px; font-weight:700; padding:11px 18px; border-radius:9px; cursor:{depositLines.length === 0 ? 'not-allowed' : 'pointer'}; opacity:{depositLines.length === 0 ? 0.5 : 1};">Déposer tout</button>
          </div>
        </aside>
      </div>
    {/if}

    {#if canOperate && !inventory}
      <Fab label="Déposer des objets" onclick={openDepositCart} />
    {/if}
  </div>
{/if}
