<script>
  import { onMount, onDestroy } from 'svelte'
  import PageHeader from '../components/PageHeader.svelte'
  import markers from '../lib/data/markers.json'

  // Types de points d'intérêt RP : libellé + couleur.
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

  let mapEl
  let map = null
  const layers = {}
  let active = $state(Object.fromEntries(Object.keys(TYPES).map((t) => [t, true])))

  onMount(async () => {
    // Leaflet touche window/document → import dynamique côté client (code-split).
    const L = (await import('leaflet')).default
    await import('leaflet/dist/leaflet.css')
    const meta = await fetch('/map/meta.json').then((r) => r.json())
    const { width: W, height: H, tileSize, maxZoom } = meta

    map = L.map(mapEl, { crs: L.CRS.Simple, preferCanvas: true, minZoom: 0, maxZoom })
    // CRS.Simple : 1 unité = 1 pixel à maxZoom. On projette les pixels natifs en LatLng.
    const sw = map.unproject([0, H], maxZoom)
    const ne = map.unproject([W, 0], maxZoom)
    const bounds = new L.LatLngBounds(sw, ne)
    L.tileLayer('/map/tiles/{z}/{x}/{y}.png', { tileSize, minZoom: 0, maxZoom, noWrap: true, bounds }).addTo(map)
    map.setMaxBounds(bounds)
    map.fitBounds(bounds)
    map.attributionControl.addAttribution('Carte © <a href="https://en.uesp.net/" target="_blank" rel="noopener">UESP</a> — CC-BY-SA 2.5')

    // Clic → log des coordonnées natives (pour caler les marqueurs ; cf. README).
    map.on('click', (e) => {
      const p = map.project(e.latlng, maxZoom)
      console.log(`Coords carte : x=${Math.round(p.x)}, y=${Math.round(p.y)}`)
    })

    for (const t of Object.keys(TYPES)) layers[t] = L.layerGroup().addTo(map)
    for (const m of markers) {
      const type = TYPES[m.type] ? m.type : 'camp'
      const cfg = TYPES[type]
      const icon = L.divIcon({
        className: 'bordeciel-marker',
        html: `<span style="display:flex;align-items:center;gap:5px;transform:translate(-8px,-8px);white-space:nowrap;">
          <span style="width:14px;height:14px;border-radius:50%;background:${cfg.color};border:2px solid #fff;box-shadow:0 0 5px rgba(0,0,0,.7);"></span>
          <span style="color:#fff;font-size:12px;font-weight:700;text-shadow:0 1px 3px #000;">${m.nom_fr}</span></span>`,
        iconSize: [0, 0],
      })
      L.marker(map.unproject([m.x, m.y], maxZoom), { icon })
        .bindPopup(
          `<strong>${m.nom_fr}</strong><br><span style="opacity:.65">${cfg.label}</span>` +
            (m.description ? `<br>${m.description}` : '') +
            (m.faction ? `<br><em>${m.faction}</em>` : ''),
        )
        .addTo(layers[type])
    }
  })

  onDestroy(() => map?.remove())

  function toggle(t) {
    active[t] = !active[t]
    if (!map) return
    if (active[t]) layers[t].addTo(map)
    else layers[t].remove()
  }
</script>

<PageHeader title="Carte de Bordeciel" description="Points d'intérêt RP — clic pour les coordonnées, filtre par type." />

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

<div bind:this={mapEl} style="height:calc(100vh - 230px); min-height:420px; width:100%; border-radius:12px; overflow:hidden; border:1px solid var(--border); background:#14283a;"></div>
