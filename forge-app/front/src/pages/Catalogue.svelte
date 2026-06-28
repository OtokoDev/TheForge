<script>
  import { me, currentBusinessId } from '../lib/session.js'
  import { api, ApiError } from '../lib/api.js'
  import { canAdminBusiness } from '../lib/roles.js'
  import { notifyError } from '../lib/notifications.js'
  import CatalogueRow from '../components/catalogue/CatalogueRow.svelte'
  import Checkbox from '../components/ui/Checkbox.svelte'

  const ORANGE = '#E8590C'
  const TEXT = '#F4F1EE'
  const MUTED = '#8f8880'
  const CARD = '#1c1a18'
  const TABLE_BG = '#1a1816'
  const HEAD_BG = '#221f1b'
  const BORDER = '1px solid rgba(255,255,255,0.07)'
  const th = `color:${MUTED}; font-weight:600; font-size:12px; letter-spacing:.03em; padding:12px 16px; border-bottom:${BORDER}; white-space:nowrap; text-align:left;`
  const thR = th + 'text-align:right;'

  let canEdit = $derived($currentBusinessId ? canAdminBusiness($me, $currentBusinessId) : false)
  let items = $state([])
  let products = $state(new Map())
  let costs = $state(new Map())
  let query = $state('')
  let fam = $state('all')
  let mat = $state('all')
  let onlySellable = $state(false)

  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function load() {
    const id = $currentBusinessId
    if (!id) return
    api('/api/catalog/items').then((v) => (items = v)).catch(fail)
    api(`/api/businesses/${id}/products`).then((rows) => (products = new Map(rows.map((p) => [p.itemId, p])))).catch(fail)
    api(`/api/businesses/${id}/costs`).then((rows) => (costs = new Map(rows.map((c) => [c.itemId, c.cost])))).catch(fail)
  }
  $effect(() => {
    $currentBusinessId
    load()
  })

  function dedupe(arr, idKey, nameKey) {
    const m = new Map()
    for (const i of arr) if (i[idKey] && i[nameKey]) m.set(i[idKey], i[nameKey])
    return Array.from(m, ([id, nom]) => ({ id, nom }))
  }

  let visible = $derived(items.filter((i) => !i.system))
  let fams = $derived(dedupe(visible, 'familyId', 'familyName'))
  let mats = $derived(dedupe(visible, 'materialId', 'materialName'))
  let rows = $derived.by(() => {
    const q = query.trim().toLowerCase()
    return visible.filter(
      (i) =>
        (fam === 'all' || i.familyId === fam) &&
        (mat === 'all' || i.materialId === mat) &&
        (!onlySellable || products.get(i.id)?.prixRevente != null) &&
        (q === '' || i.name.toLowerCase().includes(q)),
    )
  })

  const chip = (active) =>
    `background:${active ? ORANGE : '#1f1d1b'}; color:${active ? '#fff' : '#cfc8c2'}; border:${active ? `1px solid ${ORANGE}` : '1px solid rgba(255,255,255,0.08)'}; border-radius:8px; padding:5px 11px; font-size:12px; font-weight:600; cursor:pointer;`
</script>

{#snippet filterRow(labelText, current, pick, options)}
  {#if options.length > 0}
    <div style="display:flex; gap:6px; flex-wrap:wrap; align-items:center;">
      <span style="color:{MUTED}; font-size:11px; text-transform:uppercase; letter-spacing:.05em; width:64px;">{labelText}</span>
      <button onclick={() => pick('all')} style={chip(current === 'all')}>Tous</button>
      {#each options as o (o.id)}
        <button onclick={() => pick(o.id)} style={chip(current === o.id)}>{o.nom}</button>
      {/each}
    </div>
  {/if}
{/snippet}

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut) pour gérer son catalogue.</p>
{:else}
  <div>
    <div style="margin-bottom:14px;">
      <div style="color:{TEXT}; font-size:24px; font-weight:700;">Catalogue</div>
      <div style="color:{MUTED}; font-size:13.5px; margin-top:3px;">
        Valeur (coût) et prix de revente par produit{canEdit ? '' : ' — lecture seule (admin requis)'}.
      </div>
    </div>

    <div style="display:flex; flex-direction:column; gap:8px; margin-bottom:14px;">
      <div style="display:flex; align-items:center; gap:11px; flex-wrap:wrap;">
        <input
          bind:value={query}
          placeholder="Rechercher un produit…"
          style="background:{CARD}; border:1px solid rgba(255,255,255,0.1); border-radius:9px; color:{TEXT}; font-size:13.5px; padding:9px 12px; width:240px; outline:none;"
        />
        <Checkbox bind:checked={onlySellable} label="Vendables uniquement" />
      </div>
      {@render filterRow('Famille', fam, (v) => (fam = v), fams)}
      {@render filterRow('Matériau', mat, (v) => (mat = v), mats)}
    </div>

    <div style="overflow:auto; border:{BORDER}; border-radius:12px; background:{TABLE_BG};">
      <table style="width:100%; border-collapse:collapse; font-size:13.5px;">
        <thead>
          <tr style="background:{HEAD_BG};">
            <th style={th}>OBJET</th>
            <th style={th}>FAMILLE</th>
            <th style={th}>MATÉRIAU</th>
            <th style={thR}>VALEUR (COÛT)</th>
            <th style={thR}>PRIX DE REVENTE</th>
          </tr>
        </thead>
        <tbody>
          {#each rows as it (it.id)}
            <CatalogueRow businessId={$currentBusinessId} item={it} product={products.get(it.id)} cost={costs.get(it.id)} {canEdit} onSaved={load} />
          {/each}
        </tbody>
      </table>
      <div style="padding:11px 16px; color:#6f6862; font-size:12.5px; border-top:1px solid rgba(255,255,255,0.05);">{rows.length} produits</div>
    </div>
  </div>
{/if}
