<script>
  import { onMount, onDestroy } from 'svelte'
  import { me, currentBusinessId, currentBusiness } from '../lib/session.js'
  import { canOperateBusiness } from '../lib/roles.js'
  import { onRealtime } from '../lib/realtime.js'
  import { api, ApiError } from '../lib/api.js'
  import { notifyError, notifySuccess } from '../lib/notifications.js'
  import PageHeader from '../components/PageHeader.svelte'
  import staticMarkers from '../lib/data/markers.json'

  const TYPES = {
    cite: { label: 'Cité', color: '#E8590C' },
    contree: { label: 'Contrée', color: '#7aa7ff' },
    village: { label: 'Village', color: '#5BBF73' },
    fort: { label: 'Fort', color: '#c9a227' },
    donjon: { label: 'Donjon', color: '#b86bd9' },
    camp: { label: 'Camp RP', color: '#ed8472' },
    filon: { label: 'Filon / Mine', color: '#9a938c' },
    chasse: { label: 'Zone de chasse', color: '#88c088' },
  }

  let isCompagnie = $derived($currentBusiness?.type === 'COMPAGNIE')
  let canOperate = $derived($currentBusinessId ? canOperateBusiness($me, $currentBusinessId) : false)

  let mapEl = $state(null)
  let map = null
  let L = null
  let maxZoom = 5
  const layers = {}
  let unsub = null

  let active = $state(Object.fromEntries(Object.keys(TYPES).map((t) => [t, true])))
  let points = $state([]) // POI dynamiques (backend, par compagnie)
  let pending = $state(null) // { x, y } clic en attente d'ajout
  let newType = $state('camp')
  let newLabel = $state('')
  let newNote = $state('')

  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  onMount(() => {
    if (isCompagnie) initMap()
    unsub = onRealtime('MAP', loadPoints)
    return () => unsub?.()
  })
  onDestroy(() => map?.remove())

  // Recharge les POI quand on change de business (compagnie → compagnie).
  $effect(() => {
    $currentBusinessId
    if (map) loadPoints()
  })

  async function initMap() {
    L = (await import('leaflet')).default
    await import('leaflet/dist/leaflet.css')
    const meta = await fetch('/map/meta.json').then((r) => r.json())
    maxZoom = meta.maxZoom
    map = L.map(mapEl, { crs: L.CRS.Simple, preferCanvas: true, minZoom: 0, maxZoom })
    const sw = map.unproject([0, meta.height], maxZoom)
    const ne = map.unproject([meta.width, 0], maxZoom)
    const bounds = new L.LatLngBounds(sw, ne)
    L.tileLayer('/map/tiles/{z}/{x}/{y}.png', { tileSize: meta.tileSize, minZoom: 0, maxZoom, noWrap: true, bounds }).addTo(map)
    map.setMaxBounds(bounds)
    map.fitBounds(bounds)
    map.attributionControl.addAttribution('Carte © <a href="https://en.uesp.net/" target="_blank" rel="noopener">UESP</a> — CC-BY-SA 2.5')
    for (const t of Object.keys(TYPES)) layers[t] = L.layerGroup().addTo(map)

    map.on('click', (e) => {
      const p = map.project(e.latlng, maxZoom)
      const x = Math.round(p.x)
      const y = Math.round(p.y)
      console.log(`Coords carte : x=${x}, y=${y}`)
      if (canOperate) pending = { x, y }
    })

    renderAll()
    loadPoints()
  }

  async function loadPoints() {
    const id = $currentBusinessId
    if (!id || !isCompagnie) return
    try {
      points = await api(`/api/businesses/${id}/map-points`)
      renderAll()
    } catch (e) {
      fail(e)
    }
  }

  function renderAll() {
    if (!map) return
    for (const t of Object.keys(TYPES)) layers[t]?.clearLayers()
    for (const m of staticMarkers) addMarker(m, false)
    for (const p of points) addMarker(p, true)
  }

  function addMarker(m, dynamic) {
    const type = TYPES[m.type] ? m.type : 'camp'
    const cfg = TYPES[type]
    const name = m.nom_fr ?? m.label
    const icon = L.divIcon({
      className: '',
      iconSize: [0, 0],
      html: `<span style="display:flex;align-items:center;gap:5px;transform:translate(-8px,-8px);white-space:nowrap;">
        <span style="width:14px;height:14px;border-radius:50%;background:${cfg.color};border:2px solid #fff;box-shadow:0 0 5px rgba(0,0,0,.7);"></span>
        <span style="color:#fff;font-size:12px;font-weight:700;text-shadow:0 1px 3px #000;">${name}</span></span>`,
    })
    let html = `<strong>${name}</strong><br><span style="opacity:.65">${cfg.label}</span>` +
      (m.note ? `<br>${m.note}` : '') +
      (m.faction ? `<br><em>${m.faction}</em>` : '')
    if (dynamic && canOperate) html += `<br><button id="mapdel-${m.id}" style="margin-top:6px;color:#ed8472;background:none;border:none;cursor:pointer;padding:0;">Supprimer</button>`
    const mk = L.marker(map.unproject([m.x, m.y], maxZoom), { icon }).bindPopup(html)
    if (dynamic && canOperate) {
      mk.on('popupopen', () => {
        const b = document.getElementById(`mapdel-${m.id}`)
        if (b) b.onclick = () => del(m.id)
      })
    }
    mk.addTo(layers[type])
  }

  function toggle(t) {
    active[t] = !active[t]
    if (!map) return
    if (active[t]) layers[t].addTo(map)
    else layers[t].remove()
  }

  async function addPoint() {
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
      loadPoints()
    } catch (e) {
      fail(e)
    }
  }
  async function del(id) {
    if (!confirm('Supprimer ce point ?')) return
    try {
      await api(`/api/businesses/${$currentBusinessId}/map-points/${id}`, { method: 'DELETE' })
      loadPoints()
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
    {#each Object.entries(TYPES) as [t, cfg] (t)}
      <button
        onclick={() => toggle(t)}
        class="flex items-center gap-2 rounded-full border px-3 py-1 text-sm font-medium transition {active[t]
          ? 'border-border bg-muted/60 text-foreground'
          : 'border-border bg-transparent text-muted-foreground opacity-50'}"
      >
        <span class="size-2.5 rounded-full" style="background:{cfg.color};"></span>{cfg.label}
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
            {#each Object.entries(TYPES) as [t, cfg] (t)}<option value={t}>{cfg.label}</option>{/each}
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
  </div>
  {#if canOperate}<p class="mt-2 text-xs text-muted-foreground">Clique sur la carte pour ajouter un point. Les points apparaissent en direct pour toute la compagnie.</p>{/if}
{/if}
