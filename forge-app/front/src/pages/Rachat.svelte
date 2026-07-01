<script>
  import { Search, Plus, ChevronLeft, Info, Check, X, Boxes } from '@lucide/svelte'
  import { me, currentBusinessId, currentBusiness } from '../lib/session.js'
  import { api, ApiError } from '../lib/api.js'
  import { canOperateBusiness } from '../lib/roles.js'
  import { notifySuccess, notifyError } from '../lib/notifications.js'

  const ORANGE = '#E8590C', SOFT = '#f5a06a', TEXT = '#F4F1EE', MUTED = '#8f8880'
  const CARD = '#1c1a18', TABLE_BG = '#1a1816', HEAD_BG = '#221f1b', PANEL = '#1a1613'
  const INPUT_BG = '#15110e', BORDER = '1px solid rgba(255,255,255,0.07)', DEFAULT_CAT = '#7d90a6'
  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  const initials = (n) => {
    const w = (n ?? '?').trim().split(/[ \-_@]/).filter(Boolean)
    return ((w[0] || '?').charAt(0) + (w[1] || '').charAt(0)).toUpperCase()
  }

  let canOperate = $derived($currentBusinessId ? canOperateBusiness($me, $currentBusinessId) : false)

  let view = $state('registre') // 'registre' | 'caisse'
  let farmers = $state([])
  let items = $state([])
  let costs = $state(new Map())
  let defaults = $state({ stockAccountId: null, coffreAccountId: null })

  // registre
  let query = $state('')
  let filter = $state('credit') // credit | all | solde
  let sort = $state({ key: 'solde', dir: 'desc' })

  // panneau paiement
  let payFarmer = $state(null) // { farmerName, remaining, ... }
  let payAmount = $state('')
  let payEntries = $state([])

  // caisse de rachat
  let rFarmerName = $state('') // nom libre
  let rQuery = $state('')
  let rCat = $state('all')
  let cart = $state({}) // itemId -> qty
  let prices = $state({}) // itemId -> prix d'achat unitaire (éditable)
  let rMotif = $state('')

  function load() {
    const id = $currentBusinessId
    if (!id) return
    api(`/api/businesses/${id}/creances`).then((v) => (farmers = v)).catch(fail)
    api('/api/catalog/items').then((rows) => (items = rows.filter((i) => !i.system))).catch(fail)
    api(`/api/businesses/${id}/costs`).then((rows) => (costs = new Map(rows.map((c) => [c.itemId, c.cost])))).catch(fail)
    api(`/api/businesses/${id}/defaults`).then((v) => (defaults = v)).catch(fail)
  }
  $effect(() => {
    $currentBusinessId
    load()
  })

  // ── KPIs ──
  let totalDu = $derived(farmers.reduce((t, f) => t + f.remaining, 0))
  let crediteurs = $derived(farmers.filter((f) => f.remaining > 0).length)
  let totalRachat = $derived(farmers.reduce((t, f) => t + f.totalCredit, 0))
  let totalPaye = $derived(farmers.reduce((t, f) => t + f.totalPaid, 0))

  // ── registre rows ──
  let rows = $derived.by(() => {
    const q = query.trim().toLowerCase()
    let r = farmers.filter((f) => {
      if (filter === 'credit') return f.remaining > 0
      if (filter === 'solde') return f.remaining === 0
      return true
    })
    if (q) r = r.filter((f) => f.farmerName.toLowerCase().includes(q))
    const dir = sort.dir === 'asc' ? 1 : -1
    return [...r].sort((a, b) => {
      if (sort.key === 'name') return a.farmerName.localeCompare(b.farmerName) * dir
      if (sort.key === 'rachat') return (a.totalCredit - b.totalCredit) * dir
      if (sort.key === 'paye') return (a.totalPaid - b.totalPaid) * dir
      return (a.remaining - b.remaining) * dir
    })
  })
  const arrow = (k) => (sort.key === k ? (sort.dir === 'asc' ? '▲' : '▼') : '')
  function toggleSort(k) {
    sort = sort.key === k ? { key: k, dir: sort.dir === 'asc' ? 'desc' : 'asc' } : { key: k, dir: 'desc' }
  }
  const chips = $derived([
    { id: 'credit', l: 'Créditeurs', c: farmers.filter((f) => f.remaining > 0).length },
    { id: 'all', l: 'Tous', c: farmers.length },
    { id: 'solde', l: 'Soldés', c: farmers.filter((f) => f.remaining === 0).length },
  ])

  // ── caisse : catalogue ──
  function dedupeFam(list) {
    const m = new Map()
    for (const i of list) if (i.familyId && i.familyName) m.set(i.familyId, i.familyName)
    return Array.from(m, ([id, nom]) => ({ id, nom }))
  }
  let fams = $derived(dedupeFam(items))
  let catalogue = $derived.by(() => {
    const q = rQuery.trim().toLowerCase()
    return items.filter((i) => (rCat === 'all' || i.familyId === rCat) && (q === '' || i.name.toLowerCase().includes(q)))
  })
  let itemById = $derived(new Map(items.map((i) => [i.id, i])))
  let lot = $derived(Object.entries(cart).map(([id, qty]) => ({ id, item: itemById.get(id), qty, price: prices[id] ?? costs.get(id) ?? 0 })))
  let rTotal = $derived(lot.reduce((t, l) => t + l.price * l.qty, 0))
  let rUnits = $derived(lot.reduce((t, l) => t + l.qty, 0))

  function addMat(id) {
    cart = { ...cart, [id]: (cart[id] ?? 0) + 1 }
    if (prices[id] === undefined) prices = { ...prices, [id]: costs.get(id) ?? 0 }
  }
  function incMat(id, d) {
    const c = { ...cart }
    c[id] = (c[id] ?? 0) + d
    if (c[id] <= 0) delete c[id]
    cart = c
  }
  function setPrice(id, v) {
    prices = { ...prices, [id]: Math.max(0, Number(v) || 0) }
  }
  function openCaisse(farmer) {
    rFarmerName = farmer ? farmer.farmerName : ''
    rQuery = ''
    rCat = 'all'
    cart = {}
    prices = {}
    rMotif = ''
    view = 'caisse'
  }
  async function deposit(thenPay) {
    const name = rFarmerName.trim()
    if (!name) return notifyError('Nom du farmeur requis')
    const lines = lot.map((l) => ({ itemId: l.id, quantity: l.qty, unitPrice: l.price }))
    if (lines.length === 0) return notifyError('Ajoute au moins une matière')
    if (!defaults.stockAccountId) return notifyError('Aucun coffre principal (Configuration)')
    try {
      await api(`/api/businesses/${$currentBusinessId}/creances/deposit`, {
        method: 'POST',
        body: JSON.stringify({ farmerName: name, lines, stockAccountId: defaults.stockAccountId, reference: rMotif || null }),
      })
      notifySuccess('Rachat enregistré')
      view = 'registre'
      cart = {}
      prices = {}
      await refresh()
      const fr = farmers.find((f) => f.farmerName === name)
      if (thenPay && fr) openPay(fr)
    } catch (e) {
      fail(e)
    }
  }

  // ── paiement ──
  function openPay(f) {
    payFarmer = f
    payAmount = ''
    payEntries = []
    api(`/api/businesses/${$currentBusinessId}/creances/entries?farmerName=${encodeURIComponent(f.farmerName)}`)
      .then((v) => (payEntries = v)).catch(fail)
  }
  function solderTout() {
    payAmount = String(payFarmer.remaining)
  }
  async function pay() {
    const n = Number(payAmount)
    if (!Number.isFinite(n) || n <= 0) return notifyError('Montant invalide')
    if (!defaults.coffreAccountId) return notifyError('Aucun coffre principal (Configuration)')
    try {
      await api(`/api/businesses/${$currentBusinessId}/creances/payment`, {
        method: 'POST',
        body: JSON.stringify({ farmerName: payFarmer.farmerName, amount: n, coffreAccountId: defaults.coffreAccountId, reference: null }),
      })
      notifySuccess('Paiement enregistré')
      payFarmer = null
      refresh()
    } catch (e) {
      fail(e)
    }
  }
  async function refresh() {
    const id = $currentBusinessId
    if (id) farmers = await api(`/api/businesses/${id}/creances`).catch(() => farmers)
  }
  const famColor = (i) => i?.familyColor ?? DEFAULT_CAT
  const fmtDate = (iso) => new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
</script>

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut) pour gérer les créances.</p>
{:else if $currentBusiness?.type !== 'COMPAGNIE'}
  <p class="text-sm text-muted-foreground">Les créances ne concernent que les business de type <strong>Compagnie</strong>.</p>
{:else if view === 'caisse'}
  <!-- ===== CAISSE DE RACHAT ===== -->
  <div style="margin:-12px -8px 0;">
    <div style="display:flex;align-items:center;justify-content:space-between;padding:0 0 14px;border-bottom:{BORDER};">
      <div style="display:flex;align-items:center;gap:12px;">
        <button onclick={() => (view = 'registre')} style="display:flex;align-items:center;gap:7px;background:transparent;border:none;color:{MUTED};font-size:13px;font-weight:600;cursor:pointer;"><ChevronLeft size={17} />Créances</button>
        <div style="width:1px;height:20px;background:rgba(255,255,255,0.1);"></div>
        <div style="color:{TEXT};font-size:18px;font-weight:700;">Nouveau rachat</div>
      </div>
      <div style="display:flex;align-items:center;gap:8px;color:{MUTED};font-size:12.5px;"><Info size={14} />Prix d'achat éditable par ligne</div>
    </div>

    <div style="display:flex;flex-wrap:wrap;gap:0 18px;align-items:stretch;min-height:560px;">
      <!-- catalogue -->
      <div style="flex:1 1 320px;min-width:0;display:flex;flex-direction:column;padding:18px 0 4px;gap:13px;">
        <div style="position:relative;">
          <Search size={16} color={MUTED} style="position:absolute;left:11px;top:50%;transform:translateY(-50%);" />
          <input bind:value={rQuery} placeholder="Chercher une matière…" aria-label="Chercher une matière" style="width:100%;background:{CARD};border:1px solid rgba(255,255,255,0.1);border-radius:9px;color:{TEXT};font-size:13.5px;padding:10px 12px 10px 34px;outline:none;" />
        </div>
        <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
          {#each [{ id: 'all', nom: 'Toutes' }, ...fams] as c (c.id)}
            {@const active = rCat === c.id}
            <button onclick={() => (rCat = c.id)} style="background:{active ? ORANGE : '#1f1d1b'};color:{active ? '#fff' : '#cfc8c2'};border:{active ? `1px solid ${ORANGE}` : '1px solid rgba(255,255,255,0.08)'};border-radius:8px;padding:6px 12px;font-size:12.5px;font-weight:600;cursor:pointer;">{c.nom}</button>
          {/each}
        </div>
        <div style="flex:1;min-height:0;overflow:auto;display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:10px;align-content:start;">
          {#each catalogue as p (p.id)}
            <button onclick={() => addMat(p.id)} style="text-align:left;background:{CARD};border:{BORDER};border-radius:11px;padding:13px;cursor:pointer;display:flex;flex-direction:column;gap:10px;">
              <div style="display:flex;align-items:center;gap:10px;">
                <div style="width:34px;height:34px;border-radius:9px;background:{famColor(p)};display:flex;align-items:center;justify-content:center;color:#16110d;font-weight:800;font-size:14px;flex:none;">{p.name.slice(0, 2).toUpperCase()}</div>
                <div style="line-height:1.25;min-width:0;">
                  <div style="color:{TEXT};font-weight:600;font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{p.name}</div>
                  <div style="color:{MUTED};font-size:11.5px;">{p.familyName ?? '—'}</div>
                </div>
              </div>
              <div style="display:flex;align-items:baseline;justify-content:space-between;">
                <span style="color:#9a938c;font-size:11.5px;">coût indicatif</span>
                <span style="color:{TEXT};font-weight:700;font-size:14px;">{fmt(costs.get(p.id) ?? 0)} <span style="color:{MUTED};font-size:11px;font-weight:500;">/ u</span></span>
              </div>
            </button>
          {/each}
        </div>
      </div>

      <!-- lot -->
      <div style="width:438px;flex:1 1 360px;max-width:100%;display:flex;flex-direction:column;background:{PANEL};border:{BORDER};border-radius:12px;">
        <div style="padding:18px 20px 12px;">
          <div style="color:{MUTED};font-size:11.5px;text-transform:uppercase;letter-spacing:.06em;font-weight:600;margin-bottom:9px;">Farmeur crédité</div>
          <input bind:value={rFarmerName} placeholder="Nom du farmeur (texte libre)…" aria-label="Nom du farmeur" style="width:100%;background:{INPUT_BG};border:1px solid rgba(255,255,255,0.12);border-radius:9px;color:{TEXT};font-size:14px;padding:10px 12px;outline:none;" />
        </div>
        <div style="flex:1;min-height:0;overflow:auto;padding:4px 20px;">
          {#if lot.length === 0}
            <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px;min-height:200px;color:#6f6862;text-align:center;">
              <Boxes size={34} stroke-width={1.6} />
              <div style="font-size:13.5px;">Clique une matière pour l'ajouter au lot</div>
            </div>
          {:else}
            {#each lot as l (l.id)}
              <div style="display:flex;align-items:center;gap:11px;padding:11px 0;border-bottom:1px solid rgba(255,255,255,0.06);">
                <div style="width:32px;height:32px;border-radius:8px;background:{famColor(l.item)};display:flex;align-items:center;justify-content:center;color:#16110d;font-weight:800;font-size:13px;flex:none;">{(l.item?.name ?? '?').slice(0, 2).toUpperCase()}</div>
                <div style="flex:1;min-width:0;line-height:1.3;">
                  <div style="color:{TEXT};font-weight:600;font-size:13.5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{l.item?.name}</div>
                  <div style="display:flex;align-items:center;gap:5px;margin-top:3px;">
                    <input type="number" min="0" value={l.price} onchange={(e) => setPrice(l.id, e.currentTarget.value)} aria-label="Prix d'achat unitaire" style="width:72px;background:{INPUT_BG};border:1px solid rgba(255,255,255,0.12);border-radius:6px;color:{TEXT};font-size:12.5px;font-weight:600;text-align:right;padding:4px 7px;outline:none;" />
                    <span style="color:{MUTED};font-size:11.5px;">septims / u</span>
                  </div>
                </div>
                <div style="display:flex;align-items:center;gap:5px;flex:none;">
                  <button onclick={() => incMat(l.id, -1)} style="width:28px;height:28px;background:#232120;border:1px solid rgba(255,255,255,0.1);border-radius:7px;color:#cfc8c2;font-size:16px;cursor:pointer;">−</button>
                  <span style="min-width:34px;text-align:center;color:{TEXT};font-weight:700;font-size:14px;">{l.qty}</span>
                  <button onclick={() => incMat(l.id, 1)} style="width:28px;height:28px;background:#232120;border:1px solid rgba(255,255,255,0.1);border-radius:7px;color:#cfc8c2;font-size:16px;cursor:pointer;">+</button>
                </div>
                <div style="width:78px;text-align:right;color:{TEXT};font-weight:700;font-size:13.5px;flex:none;">{fmt(l.price * l.qty)}</div>
              </div>
            {/each}
          {/if}
        </div>
        <div style="border-top:1px solid rgba(255,255,255,0.08);padding:16px 20px;background:#19110d;border-radius:0 0 12px 12px;">
          <div style="display:flex;justify-content:space-between;color:#9a938c;font-size:13px;margin-bottom:6px;"><span>{lot.length} matières · {fmt(rUnits)} unités</span><span>prix d'achat</span></div>
          <div style="display:flex;justify-content:space-between;align-items:baseline;margin-bottom:14px;"><span style="color:{TEXT};font-size:15px;font-weight:700;">Crédité au farmeur</span><span style="color:{ORANGE};font-size:27px;font-weight:800;">{fmt(rTotal)} <span style="font-size:13px;color:{MUTED};font-weight:500;">septims</span></span></div>
          <div style="display:flex;align-items:center;gap:10px;background:#15110e;border:1px solid rgba(255,255,255,0.08);border-radius:9px;padding:11px 13px;margin-bottom:14px;color:{MUTED};font-size:12px;">
            <Info size={15} /> La marchandise entre dans le coffre principal ; le farmeur est crédité du total.
          </div>
          <div style="color:{MUTED};font-size:11.5px;text-transform:uppercase;letter-spacing:.06em;font-weight:600;margin-bottom:8px;">Motif <span style="text-transform:none;letter-spacing:0;color:#6f6862;">(optionnel)</span></div>
          <input bind:value={rMotif} placeholder="Ex. récolte de la semaine…" style="width:100%;background:#15110e;border:1px solid rgba(255,255,255,0.1);border-radius:8px;color:{TEXT};font-size:13px;padding:10px 12px;outline:none;margin-bottom:13px;" />
          <button onclick={() => deposit(false)} disabled={!canOperate} style="width:100%;background:{ORANGE};border:none;color:#fff;font-size:15px;font-weight:700;padding:14px;border-radius:11px;cursor:{canOperate ? 'pointer' : 'not-allowed'};opacity:{canOperate ? 1 : 0.5};box-shadow:0 6px 18px rgba(232,89,12,0.32);display:flex;align-items:center;justify-content:center;gap:9px;">
            <Check size={18} /> Enregistrer le rachat · crédite {fmt(rTotal)}
          </button>
          <button onclick={() => deposit(true)} disabled={!canOperate} style="width:100%;margin-top:9px;background:transparent;border:1px solid rgba(255,255,255,0.12);color:#cfc8c2;font-size:13px;font-weight:600;padding:10px;border-radius:9px;cursor:pointer;">Enregistrer &amp; payer immédiatement</button>
        </div>
      </div>
    </div>
  </div>
{:else}
  <!-- ===== REGISTRE ===== -->
  <div>
    <div style="display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:15px;">
      <div>
        <div style="color:{TEXT};font-size:24px;font-weight:700;letter-spacing:-0.01em;">Créances</div>
        <div style="color:{MUTED};font-size:13.5px;margin-top:3px;">Rachat de matières aux farmeurs · {crediteurs} farmeur{crediteurs > 1 ? 's' : ''} à payer</div>
      </div>
      {#if canOperate}
        <div style="display:flex;gap:10px;">
          <button onclick={() => openCaisse(null)} style="display:flex;align-items:center;gap:8px;background:{ORANGE};border:none;color:#fff;font-size:13.5px;font-weight:700;padding:10px 17px;border-radius:9px;cursor:pointer;box-shadow:0 4px 14px rgba(232,89,12,0.32);"><Plus size={17} /> Racheter des matières</button>
        </div>
      {/if}
    </div>

    <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px;margin-bottom:15px;">
      <div style="background:{CARD};border:1px solid rgba(232,89,12,0.32);border-radius:12px;padding:15px 17px;">
        <div style="color:{MUTED};font-size:11.5px;text-transform:uppercase;letter-spacing:.06em;font-weight:600;">Total dû aux farmeurs</div>
        <div style="color:{SOFT};font-size:25px;font-weight:700;margin-top:6px;">{fmt(totalDu)} <span style="font-size:13px;color:{MUTED};font-weight:500;">septims</span></div>
      </div>
      {#each [{ l: 'Farmeurs créditeurs', v: String(crediteurs) }, { l: 'Total racheté', v: `${fmt(totalRachat)}` }, { l: 'Total payé', v: `${fmt(totalPaye)}` }] as kp (kp.l)}
        <div style="background:{CARD};border:{BORDER};border-radius:12px;padding:15px 17px;">
          <div style="color:{MUTED};font-size:11.5px;text-transform:uppercase;letter-spacing:.06em;font-weight:600;">{kp.l}</div>
          <div style="color:{TEXT};font-size:25px;font-weight:700;margin-top:6px;">{kp.v}</div>
        </div>
      {/each}
    </div>

    <div style="display:flex;align-items:center;gap:11px;flex-wrap:wrap;margin-bottom:15px;">
      <div style="position:relative;">
        <Search size={16} color={MUTED} style="position:absolute;left:11px;top:50%;transform:translateY(-50%);" />
        <input bind:value={query} placeholder="Rechercher un farmeur…" aria-label="Rechercher un farmeur" style="background:{CARD};border:1px solid rgba(255,255,255,0.1);border-radius:9px;color:{TEXT};font-size:13.5px;padding:9px 12px 9px 34px;width:300px;outline:none;" />
      </div>
      {#each chips as c (c.id)}
        {@const active = filter === c.id}
        <button onclick={() => (filter = c.id)} style="display:flex;align-items:center;gap:7px;background:{active ? ORANGE : '#1f1d1b'};color:{active ? '#fff' : '#cfc8c2'};border:{active ? `1px solid ${ORANGE}` : '1px solid rgba(255,255,255,0.08)'};border-radius:999px;padding:7px 13px;font-size:13px;font-weight:600;cursor:pointer;">
          {c.l}<span style="font-size:11px;font-weight:700;background:{active ? 'rgba(255,255,255,0.22)' : 'rgba(255,255,255,0.06)'};color:{active ? '#fff' : MUTED};padding:2px 6px;border-radius:5px;">{c.c}</span>
        </button>
      {/each}
    </div>

    <div style="overflow:auto;border:{BORDER};border-radius:12px;background:{TABLE_BG};">
      <table style="width:100%;border-collapse:collapse;font-size:13.5px;">
        <thead>
          <tr style="background:{HEAD_BG};">
            {#each [{ k: 'name', l: 'FARMEUR', a: 'left' }, { k: 'rachat', l: 'RACHATS', a: 'right' }, { k: 'paye', l: 'PAYÉ', a: 'right' }, { k: 'solde', l: 'SOLDE DÛ', a: 'right' }] as h (h.k)}
              <th onclick={() => toggleSort(h.k)} style="text-align:{h.a};color:{MUTED};font-weight:600;font-size:12px;letter-spacing:.03em;padding:13px 16px;cursor:pointer;border-bottom:{BORDER};white-space:nowrap;">{h.l} <span style="color:{ORANGE};font-size:10px;">{arrow(h.k)}</span></th>
            {/each}
            <th style="width:170px;border-bottom:{BORDER};"></th>
          </tr>
        </thead>
        <tbody>
          {#each rows as f (f.farmerName)}
            {@const due = f.remaining > 0}
            <tr style="border-bottom:1px solid rgba(255,255,255,0.05);">
              <td style="padding:11px 16px;">
                <div style="display:flex;align-items:center;gap:11px;">
                  <div style="width:32px;height:32px;border-radius:999px;background:rgba(232,89,12,0.16);display:flex;align-items:center;justify-content:center;color:{SOFT};font-weight:700;font-size:12.5px;flex:none;">{initials(f.farmerName)}</div>
                  <div style="color:{TEXT};font-weight:600;">{f.farmerName}</div>
                </div>
              </td>
              <td style="padding:11px 16px;text-align:right;color:#9a938c;">{fmt(f.totalCredit)}</td>
              <td style="padding:11px 16px;text-align:right;color:#9a938c;">{fmt(f.totalPaid)}</td>
              <td style="padding:11px 16px;text-align:right;"><span style="color:{due ? SOFT : '#6f6862'};font-weight:700;font-size:14.5px;">{due ? fmt(f.remaining) : '— soldé'}</span></td>
              <td style="padding:11px 16px;">
                <div style="display:flex;gap:7px;justify-content:flex-end;">
                  <button onclick={() => due && openPay(f)} disabled={!due || !canOperate} style="background:{due ? 'rgba(232,89,12,0.14)' : 'transparent'};border:{due ? '1px solid rgba(232,89,12,0.4)' : '1px solid rgba(255,255,255,0.1)'};color:{due ? SOFT : '#6f6862'};font-size:12.5px;font-weight:700;padding:7px 13px;border-radius:7px;cursor:{due && canOperate ? 'pointer' : 'default'};">Payer</button>
                  {#if canOperate}
                    <button title="Racheter" onclick={() => openCaisse(f)} style="width:32px;height:32px;border-radius:7px;background:rgba(255,255,255,0.05);border:1px solid rgba(255,255,255,0.1);color:#cfc8c2;cursor:pointer;display:flex;align-items:center;justify-content:center;font-size:17px;flex:none;">+</button>
                  {/if}
                </div>
              </td>
            </tr>
          {/each}
          {#if rows.length === 0}<tr><td colspan="5" style="padding:14px 16px;color:{MUTED};">Aucun farmeur.</td></tr>{/if}
        </tbody>
      </table>
      <div style="padding:11px 16px;color:#6f6862;font-size:12.5px;border-top:1px solid rgba(255,255,255,0.05);">{rows.length} farmeurs</div>
    </div>
  </div>

  <!-- ===== PANNEAU PAIEMENT ===== -->
  {#if payFarmer}
    <div style="position:fixed;top:0;right:0;bottom:0;width:min(404px,100vw);background:{PANEL};border-left:1px solid rgba(255,255,255,0.1);box-shadow:-26px 0 55px rgba(0,0,0,0.45);display:flex;flex-direction:column;z-index:50;">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:16px 18px;border-bottom:{BORDER};">
        <div style="color:{MUTED};font-size:11.5px;text-transform:uppercase;letter-spacing:.08em;font-weight:600;">Payer un farmeur</div>
        <button onclick={() => (payFarmer = null)} aria-label="Fermer" style="background:transparent;border:none;color:{MUTED};cursor:pointer;display:flex;padding:4px;"><X size={18} /></button>
      </div>
      <div style="padding:18px;display:flex;gap:13px;align-items:center;">
        <div style="width:46px;height:46px;border-radius:999px;background:rgba(232,89,12,0.16);display:flex;align-items:center;justify-content:center;color:{SOFT};font-weight:700;font-size:17px;flex:none;">{initials(payFarmer.farmerName)}</div>
        <div style="color:{TEXT};font-size:17px;font-weight:700;">{payFarmer.farmerName}</div>
      </div>
      <div style="margin:0 18px;background:#15110e;border:1px solid rgba(232,89,12,0.3);border-radius:11px;padding:14px 16px;display:flex;align-items:center;justify-content:space-between;">
        <span style="color:{MUTED};font-size:12.5px;text-transform:uppercase;letter-spacing:.06em;font-weight:600;">Solde dû</span>
        <span style="color:{SOFT};font-size:24px;font-weight:800;">{fmt(payFarmer.remaining)} <span style="font-size:12px;color:{MUTED};font-weight:500;">septims</span></span>
      </div>
      <div style="padding:18px 18px 0;">
        <div style="color:{MUTED};font-size:11.5px;text-transform:uppercase;letter-spacing:.06em;font-weight:600;">Montant à payer</div>
        <div style="display:flex;align-items:center;gap:9px;margin-top:8px;">
          <input bind:value={payAmount} type="number" placeholder="0" style="flex:1;background:#15110e;border:1px solid rgba(255,255,255,0.12);border-radius:9px;color:{TEXT};font-size:20px;font-weight:700;text-align:right;padding:10px 13px;outline:none;" />
          <button onclick={solderTout} style="background:rgba(232,89,12,0.14);border:1px solid rgba(232,89,12,0.4);color:{SOFT};font-size:12.5px;font-weight:700;padding:11px 14px;border-radius:9px;cursor:pointer;white-space:nowrap;">Solder tout</button>
        </div>
      </div>
      <div style="padding:16px 18px 14px;">
        <button onclick={pay} disabled={!canOperate} style="width:100%;background:{ORANGE};border:none;color:#fff;font-size:14px;font-weight:700;padding:13px;border-radius:10px;cursor:{canOperate ? 'pointer' : 'not-allowed'};opacity:{canOperate ? 1 : 0.5};box-shadow:0 5px 16px rgba(232,89,12,0.28);">Payer{payAmount ? ` ${fmt(Number(payAmount) || 0)}` : ''} · sortie coffre principal</button>
      </div>
      <div style="padding:14px 18px;border-top:{BORDER};flex:1;min-height:0;overflow:auto;">
        <div style="color:{MUTED};font-size:11.5px;text-transform:uppercase;letter-spacing:.06em;font-weight:600;margin-bottom:10px;">Historique</div>
        {#each payEntries as mv, i (i)}
          {@const credit = mv.type === 'CREDIT'}
          <div style="display:flex;align-items:center;gap:11px;padding:9px 0;border-bottom:1px solid rgba(255,255,255,0.05);">
            <span style="font-size:10.5px;font-weight:700;color:{credit ? '#5BBF73' : '#E5604D'};border:1px solid {credit ? '#5BBF73' : '#E5604D'};padding:2px 7px;border-radius:5px;flex:none;">{credit ? 'RACHAT' : 'PAIEMENT'}</span>
            <div style="flex:1;min-width:0;"><div style="color:#cfc8c2;font-size:12.5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{mv.reference ?? '—'}</div><div style="color:#6f6862;font-size:11.5px;">{fmtDate(mv.createdAt)} · {mv.username}</div></div>
            <span style="color:{credit ? '#5BBF73' : '#E5604D'};font-weight:700;font-size:13.5px;flex:none;">{credit ? '+' : '−'}{fmt(mv.amount)}</span>
          </div>
        {/each}
        {#if payEntries.length === 0}<p style="color:{MUTED};font-size:12.5px;">Aucun mouvement.</p>{/if}
      </div>
    </div>
  {/if}
{/if}
