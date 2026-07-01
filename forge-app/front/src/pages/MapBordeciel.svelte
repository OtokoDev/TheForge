<script>
  import { onMount, onDestroy } from 'svelte'
  import { me, currentBusinessId, currentBusiness } from '../lib/session.js'
  import { canOperateBusiness } from '../lib/roles.js'
  import { onRealtime } from '../lib/realtime.js'
  import { api, ApiError } from '../lib/api.js'
  import { notifyError, notifySuccess } from '../lib/notifications.js'
  import PageHeader from '../components/PageHeader.svelte'
  import toponyms from '../lib/data/toponyms.json'
  import holds from '../lib/data/holds.json'

  const NEUTRAL = '#9a938c'
  let isCompagnie = $derived($currentBusiness?.type === 'COMPAGNIE')
  let canOperate = $derived($currentBusinessId ? canOperateBusiness($me, $currentBusinessId) : false)

  let mapEl = $state(null)
  let map = null
  let L = null
  let maxZoom = 5
  let markersLayer = null
  let holdOverlay = null // calque zones d'influence (svgOverlay)
  let labelOverlay = null // calque noms villes/villages (svgOverlay)
  let mapW = 0
  let mapH = 0
  let mapBounds = null
  let unsub = null

  let markerTypes = $state([]) // [{ id, label, color, imageDataUrl }]
  let points = $state([]) // POI dynamiques
  let active = $state({ _holds: false }) // key -> false si masqué (zones masquées par défaut)
  let pending = $state(null) // { x, y } clic en attente
  let editing = $state(null) // { id, type, label, note } édition d'un point existant
  let newType = $state('')
  let newLabel = $state('')
  let newNote = $state('')

  let typeById = $derived(new Map(markerTypes.map((t) => [t.id, t])))
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  const visible = (key) => active[key] !== false

  onMount(() => {
    if (isCompagnie) initMap()
    unsub = onRealtime('MAP', loadMap)
    return () => unsub?.()
  })
  onDestroy(() => map?.remove())

  $effect(() => {
    $currentBusinessId
    if (map) loadMap()
  })

  async function initMap() {
    L = (await import('leaflet')).default
    await import('leaflet/dist/leaflet.css')
    const meta = await fetch('/map/meta.json').then((r) => r.json())
    maxZoom = meta.maxZoom
    mapW = meta.width
    mapH = meta.height
    map = L.map(mapEl, { crs: L.CRS.Simple, preferCanvas: true, minZoom: 0, maxZoom })
    mapBounds = new L.LatLngBounds(map.unproject([0, meta.height], maxZoom), map.unproject([meta.width, 0], maxZoom))
    L.tileLayer('/map/tiles/{z}/{x}/{y}.png', { tileSize: meta.tileSize, minZoom: 0, maxZoom, noWrap: true, bounds: mapBounds }).addTo(map)
    map.setMaxBounds(mapBounds)
    map.fitBounds(mapBounds)
    map.attributionControl.addAttribution('Carte © <a href="https://en.uesp.net/" target="_blank" rel="noopener">UESP</a> — CC-BY-SA 2.5')
    map.createPane('holds').style.zIndex = 410
    map.createPane('labels').style.zIndex = 450
    buildOverlays()
    markersLayer = L.layerGroup().addTo(map)

    map.on('click', (e) => {
      const p = map.project(e.latlng, maxZoom)
      const x = Math.round(p.x)
      const y = Math.round(p.y)
      console.log(`Coords carte : x=${x}, y=${y}`)
      if (!canOperate) return
      if (markerTypes.length === 0) return notifyError('Configure d’abord des types de marqueurs (Configuration → Marqueurs)')
      editing = null
      pending = { x, y }
    })

    loadMap()
  }

  async function loadMap() {
    const id = $currentBusinessId
    if (!id || !isCompagnie) return
    try {
      const [types, pts] = await Promise.all([
        api(`/api/businesses/${id}/marker-types`),
        api(`/api/businesses/${id}/map-points`),
      ])
      markerTypes = types
      points = pts
      if (!newType || !types.some((t) => t.id === newType)) newType = types[0]?.id ?? ''
      renderAll()
    } catch (e) {
      fail(e)
    }
  }

  function renderAll() {
    if (!map || !markersLayer) return
    markersLayer.clearLayers()
    for (const p of points) if (visible(p.type)) addMarker(p, typeById.get(p.type))
    syncOverlays()
  }

  function addMarker(m, type) {
    const color = type?.color ?? NEUTRAL
    const img = type?.imageDataUrl
    const off = img ? 13 : 7
    const visual = img
      ? `<img src="${img}" style="width:26px;height:26px;object-fit:contain;filter:drop-shadow(0 1px 3px #000);" />`
      : `<span style="width:14px;height:14px;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 0 5px rgba(0,0,0,.7);"></span>`
    const icon = L.divIcon({
      className: '',
      iconSize: [0, 0],
      html: `<span style="display:flex;align-items:center;gap:5px;transform:translate(-${off}px,-${off}px);white-space:nowrap;">${visual}<span style="color:#fff;font-size:12px;font-weight:700;text-shadow:0 1px 3px #000;">${m.label}</span></span>`,
    })
    let html = `<strong>${m.label}</strong>` + (type ? `<br><span style="opacity:.65">${type.label}</span>` : '') + (m.note ? `<br>${m.note}` : '')
    if (canOperate) {
      html += `<br><span style="margin-top:6px;display:inline-flex;gap:12px;">` +
        `<button id="mapedit-${m.id}" style="color:#7db3ed;background:none;border:none;cursor:pointer;padding:0;">Éditer</button>` +
        `<button id="mapdel-${m.id}" style="color:#ed8472;background:none;border:none;cursor:pointer;padding:0;">Supprimer</button></span>`
    }
    const mk = L.marker(map.unproject([m.x, m.y], maxZoom), { icon }).bindPopup(html)
    if (canOperate) {
      mk.on('popupopen', () => {
        const d = document.getElementById(`mapdel-${m.id}`)
        if (d) d.onclick = () => del(m.id)
        const ed = document.getElementById(`mapedit-${m.id}`)
        if (ed) ed.onclick = () => startEditPoint(m)
      })
    }
    mk.addTo(markersLayer)
  }

  // --- Calques statiques (noms FR + zones d'influence), rendus en SVG vectoriel ---
  const svgFromString = (s) => new DOMParser().parseFromString(s, 'image/svg+xml').documentElement

  // centroïde de polygone (formule de l'aire) ; fallback = 1er point. Placement du nom de hold.
  function holdCentroid(pts) {
    let a = 0, cx = 0, cy = 0
    for (let i = 0; i < pts.length; i++) {
      const [x1, y1] = pts[i]
      const [x2, y2] = pts[(i + 1) % pts.length]
      const f = x1 * y2 - x2 * y1
      a += f
      cx += (x1 + x2) * f
      cy += (y1 + y2) * f
    }
    if (a === 0) return pts[0]
    a *= 0.5
    return [cx / (6 * a), cy / (6 * a)]
  }

  function holdsSvg() {
    const parts = holds
      .map((h) => {
        const poly = `<polygon points="${h.points.map((p) => p.join(',')).join(' ')}" fill="${h.color}" fill-opacity="0.28" stroke="${h.color}" stroke-opacity="0.85" stroke-width="10" stroke-linejoin="round"/>`
        const [cx, cy] = h.labelAt ?? holdCentroid(h.points)
        const label = `<text x="${cx}" y="${cy}" text-anchor="middle" dominant-baseline="central" font-family="Georgia, 'Times New Roman', serif" font-size="132" font-weight="700" letter-spacing="16" fill="#f8edd0" fill-opacity="0.92" stroke="#000" stroke-width="8" style="paint-order:stroke">${h.nom.toUpperCase()}</text>`
        return poly + label
      })
      .join('')
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${mapW} ${mapH}">${parts}</svg>`
  }

  function labelsSvg() {
    const t = toponyms
      .map((p) => {
        const cap = p.kind === 'capitale'
        const size = cap ? 96 : 64
        const weight = cap ? 800 : 700
        const spacing = cap ? 7 : 2
        const txt = cap ? p.nom.toUpperCase() : p.nom
        return `<text x="${p.x}" y="${p.y}" text-anchor="middle" dominant-baseline="central" font-family="Georgia, 'Times New Roman', serif" font-size="${size}" font-weight="${weight}" letter-spacing="${spacing}" fill="#f8edd0" stroke="#000000" stroke-width="${cap ? 11 : 8}" style="paint-order:stroke">${txt}</text>`
      })
      .join('')
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${mapW} ${mapH}">${t}</svg>`
  }

  function buildOverlays() {
    holdOverlay = L.svgOverlay(svgFromString(holdsSvg()), mapBounds, { interactive: false, pane: 'holds' })
    labelOverlay = L.svgOverlay(svgFromString(labelsSvg()), mapBounds, { interactive: false, pane: 'labels' })
  }

  function syncOverlays() {
    if (!map) return
    for (const [layer, show] of [
      [holdOverlay, visible('_holds')],
      [labelOverlay, visible('_labels')],
    ]) {
      if (!layer) continue
      if (show && !map.hasLayer(layer)) layer.addTo(map)
      else if (!show && map.hasLayer(layer)) map.removeLayer(layer)
    }
  }

  function toggle(key) {
    active[key] = !visible(key)
    renderAll()
  }

  async function addPoint() {
    if (!newType) return notifyError('Choisis un type')
    if (!newLabel.trim()) return notifyError('Nom requis')
    try {
      await api(`/api/businesses/${$currentBusinessId}/map-points`, {
        method: 'POST',
        body: JSON.stringify({ type: newType, label: newLabel.trim(), x: pending.x, y: pending.y, note: newNote || null }),
      })
      notifySuccess('Point ajouté')
      pending = null
      newLabel = ''
      newNote = ''
      loadMap()
    } catch (e) {
      fail(e)
    }
  }
  function startEditPoint(m) {
    map.closePopup()
    pending = null
    editing = { id: m.id, type: m.type, label: m.label, note: m.note ?? '' }
  }
  async function saveEdit() {
    if (!editing.label.trim()) return notifyError('Nom requis')
    try {
      await api(`/api/businesses/${$currentBusinessId}/map-points/${editing.id}`, {
        method: 'PUT',
        body: JSON.stringify({ type: editing.type, label: editing.label.trim(), note: editing.note || null }),
      })
      notifySuccess('Point modifié')
      editing = null
      loadMap()
    } catch (e) {
      fail(e)
    }
  }
  async function del(id) {
    if (!confirm('Supprimer ce point ?')) return
    try {
      await api(`/api/businesses/${$currentBusinessId}/map-points/${id}`, { method: 'DELETE' })
      loadMap()
    } catch (e) {
      fail(e)
    }
  }
</script>

<PageHeader title="Carte de Bordeciel" description="Points d'intérêt RP — clic pour ajouter, filtre par type." />

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut).</p>
{:else if !isCompagnie}
  <p class="text-sm text-muted-foreground">La carte est réservée aux <strong>compagnies</strong>.</p>
{:else}
  <div class="mb-3 flex flex-wrap gap-2">
    <button onclick={() => toggle('_labels')} class="flex items-center gap-2 rounded-full border px-3 py-1 text-sm font-medium transition {visible('_labels') ? 'border-border bg-muted/60 text-foreground' : 'opacity-50'}">
      <span class="text-xs font-bold tracking-tight">Aa</span>Noms
    </button>
    <button onclick={() => toggle('_holds')} class="flex items-center gap-2 rounded-full border px-3 py-1 text-sm font-medium transition {visible('_holds') ? 'border-border bg-muted/60 text-foreground' : 'opacity-50'}">
      <span class="size-2.5 rounded-sm" style="background:linear-gradient(135deg,#7a9a3b,#b0603a,#3b6ea5);"></span>Zones d'influence
    </button>
    {#each markerTypes as t (t.id)}
      <button onclick={() => toggle(t.id)} class="flex items-center gap-2 rounded-full border px-3 py-1 text-sm font-medium transition {visible(t.id) ? 'border-border bg-muted/60 text-foreground' : 'opacity-50'}">
        {#if t.imageDataUrl}<img src={t.imageDataUrl} alt="" class="size-4 rounded object-contain" />{:else}<span class="size-2.5 rounded-full" style="background:{t.color};"></span>{/if}{t.label}
      </button>
    {/each}
  </div>

  <div class="relative">
    <div bind:this={mapEl} style="height:calc(100vh - 230px); min-height:420px; width:100%; border-radius:12px; overflow:hidden; border:1px solid var(--border); background:#14283a;"></div>

    {#if pending}
      <div class="absolute right-3 top-3 z-[500] w-64 rounded-xl border bg-popover p-3 text-popover-foreground shadow-xl">
        <div class="mb-2 text-sm font-semibold">Nouveau point <span class="text-xs font-normal text-muted-foreground">({pending.x}, {pending.y})</span></div>
        <div class="flex flex-col gap-2">
          <select bind:value={newType} class="rounded-md border bg-input/30 px-2 py-1.5 text-sm">
            {#each markerTypes as t (t.id)}<option value={t.id}>{t.label}</option>{/each}
          </select>
          <input bind:value={newLabel} placeholder="Nom" aria-label="Nom" class="rounded-md border bg-input/30 px-2 py-1.5 text-sm outline-none" />
          <input bind:value={newNote} placeholder="Note (optionnel)" aria-label="Note" class="rounded-md border bg-input/30 px-2 py-1.5 text-sm outline-none" />
          <div class="flex justify-end gap-2">
            <button onclick={() => (pending = null)} class="rounded-md border px-2.5 py-1 text-sm">Annuler</button>
            <button onclick={addPoint} class="rounded-md bg-primary px-2.5 py-1 text-sm font-medium text-primary-foreground">Ajouter</button>
          </div>
        </div>
      </div>
    {/if}

    {#if editing}
      <div class="absolute right-3 top-3 z-[500] w-64 rounded-xl border bg-popover p-3 text-popover-foreground shadow-xl">
        <div class="mb-2 text-sm font-semibold">Modifier le point</div>
        <div class="flex flex-col gap-2">
          <select bind:value={editing.type} class="rounded-md border bg-input/30 px-2 py-1.5 text-sm">
            {#each markerTypes as t (t.id)}<option value={t.id}>{t.label}</option>{/each}
          </select>
          <input bind:value={editing.label} placeholder="Nom" aria-label="Nom" class="rounded-md border bg-input/30 px-2 py-1.5 text-sm outline-none" />
          <input bind:value={editing.note} placeholder="Note (optionnel)" aria-label="Note" class="rounded-md border bg-input/30 px-2 py-1.5 text-sm outline-none" />
          <div class="flex justify-end gap-2">
            <button onclick={() => (editing = null)} class="rounded-md border px-2.5 py-1 text-sm">Annuler</button>
            <button onclick={saveEdit} class="rounded-md bg-primary px-2.5 py-1 text-sm font-medium text-primary-foreground">Enregistrer</button>
          </div>
        </div>
      </div>
    {/if}
  </div>

  {#if canOperate}
    <p class="mt-2 text-xs text-muted-foreground">
      {#if markerTypes.length === 0}
        Configure des types de marqueurs dans <strong>Configuration → Marqueurs</strong> pour pouvoir poser des points.
      {:else}
        Clique sur la carte pour ajouter un point. Mise à jour en direct pour toute la compagnie.
      {/if}
    </p>
  {/if}
{/if}
