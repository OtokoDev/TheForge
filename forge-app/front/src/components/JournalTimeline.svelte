<script>
  import { api, ApiError } from '../lib/api.js'
  import { notifyError } from '../lib/notifications.js'
  import Pagination from './ui/Pagination.svelte'

  // Timeline d'événements (main courante / audit) : filtres par type (multi) + date + recherche,
  // pagination client, regroupement par jour. Calqué sur le JournalTimeline de Nexis.
  let { path, title, subtitle = '', limit = 500 } = $props()

  const LABELS = {
    LOGIN_OK: { l: 'Connexion', c: '#b450dc' },
    LOGIN_FAIL: { l: 'Échec connexion', c: '#ed8472' },
    MEMBER_ADD: { l: 'Ajout membre', c: '#5fa890' },
    ROLE_SET: { l: 'Changement de rôle', c: '#4f6ef7' },
    CREANCE_DEPOT: { l: 'Dépôt créance', c: '#E8590C' },
    CREANCE_PAIEMENT: { l: 'Paiement créance', c: '#d9a441' },
    USER_BAN: { l: 'Bannissement', c: '#ed8472' },
    USER_UNBAN: { l: 'Réactivation', c: '#5fa890' },
    BUSINESS_CREATE: { l: 'Business créé', c: '#5fa890' },
  }
  const info = (a) => LABELS[a] ?? { l: a, c: '#8f8880' }
  const fmtTime = (iso) => new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'medium' })
  const dayKey = (iso) => {
    const d = new Date(iso)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }
  const dayLabel = (iso) => new Date(iso).toLocaleDateString('fr-FR', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' })

  let entries = $state([])
  let loading = $state(true)
  let selectedTypes = $state(new Set())
  let dateFrom = $state('')
  let dateTo = $state('')
  let search = $state('')
  let page = $state(1)
  let pageSize = $state(50)

  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  async function load() {
    loading = true
    try {
      entries = await api(`${path}?limit=${limit}`)
    } catch (e) {
      fail(e)
    } finally {
      loading = false
    }
  }
  $effect(() => {
    path
    load()
  })

  let presentTypes = $derived([...new Set(entries.map((e) => e.action))])
  let filtered = $derived.by(() => {
    const q = search.trim().toLowerCase()
    return entries.filter((e) => {
      if (selectedTypes.size > 0 && !selectedTypes.has(e.action)) return false
      const j = dayKey(e.createdAt)
      if (dateFrom && j < dateFrom) return false
      if (dateTo && j > dateTo) return false
      if (q && !`${e.details ?? ''} ${e.username ?? ''} ${info(e.action).l}`.toLowerCase().includes(q)) return false
      return true
    })
  })
  let paged = $derived(filtered.slice((page - 1) * pageSize, page * pageSize))
  let grouped = $derived.by(() => {
    const out = []
    let cur = null
    for (const e of paged) {
      const d = dayLabel(e.createdAt)
      if (!cur || cur.day !== d) {
        cur = { day: d, items: [] }
        out.push(cur)
      }
      cur.items.push(e)
    }
    return out
  })
  let filtresActifs = $derived(selectedTypes.size > 0 || !!dateFrom || !!dateTo || !!search)

  function toggleType(t) {
    const s = new Set(selectedTypes)
    s.has(t) ? s.delete(t) : s.add(t)
    selectedTypes = s
    page = 1
  }
  function reset() {
    selectedTypes = new Set()
    dateFrom = ''
    dateTo = ''
    search = ''
    page = 1
  }
</script>

<div class="flex flex-col gap-3">
  <div class="flex flex-wrap items-end justify-between gap-3">
    <div>
      <h1 class="text-2xl font-bold tracking-tight">{title}</h1>
      {#if subtitle}<p class="mt-1 text-sm text-muted-foreground">{subtitle}</p>{/if}
    </div>
    <button onclick={load} class="rounded-md border px-3 py-1.5 text-sm text-muted-foreground hover:bg-muted">Actualiser</button>
  </div>

  <input type="search" placeholder="Rechercher (détail, auteur)…" bind:value={search} class="w-full rounded-lg border border-input bg-input/30 px-3 py-2 text-sm outline-none focus-visible:border-ring" />

  <div class="flex flex-wrap items-center gap-x-6 gap-y-2">
    <div class="flex flex-1 flex-wrap items-center gap-1.5">
      {#each presentTypes as t (t)}
        {@const i = info(t)}
        <button
          onclick={() => toggleType(t)}
          class="rounded-full border px-2.5 py-0.5 text-xs font-medium transition"
          style={selectedTypes.has(t) ? `border-color:${i.c}; color:${i.c}` : 'color:var(--muted-foreground)'}
        >
          {i.l}
        </button>
      {/each}
    </div>
    <div class="flex items-center gap-2 text-xs text-muted-foreground">
      <label class="flex items-center gap-1">Du <input type="date" bind:value={dateFrom} class="rounded-md border bg-card px-2 py-1" /></label>
      <label class="flex items-center gap-1">au <input type="date" bind:value={dateTo} class="rounded-md border bg-card px-2 py-1" /></label>
      {#if filtresActifs}<button onclick={reset} class="rounded-md border px-2 py-1 hover:bg-muted">Réinitialiser</button>{/if}
    </div>
  </div>

  {#if loading}
    <p class="text-sm text-muted-foreground">Chargement…</p>
  {:else}
    {#each grouped as g (g.day)}
      <div class="rounded-md border">
        <div class="sticky top-0 border-b bg-card/95 px-4 py-2 text-sm font-semibold capitalize backdrop-blur">{g.day}</div>
        <div class="flex flex-col divide-y">
          {#each g.items as e, idx (idx)}
            {@const i = info(e.action)}
            <div class="flex flex-wrap items-center gap-3 px-4 py-2 text-sm">
              <span class="w-36 shrink-0 font-mono text-xs text-muted-foreground">{fmtTime(e.createdAt)}</span>
              <span class="inline-flex min-w-28 justify-center rounded-md px-2 py-0.5 text-xs font-medium" style="background:color-mix(in srgb, {i.c} 15%, transparent); color:{i.c}">{i.l}</span>
              <span class="min-w-0 flex-1 truncate">{e.details ?? ''}</span>
              <span class="shrink-0 text-muted-foreground">{e.username}</span>
            </div>
          {/each}
        </div>
      </div>
    {/each}
    {#if filtered.length === 0}
      <p class="px-1 py-3 text-sm text-muted-foreground">{entries.length === 0 ? 'Aucun événement.' : 'Aucun événement ne correspond aux filtres.'}</p>
    {/if}
    {#if filtered.length > 0}
      <Pagination bind:page bind:pageSize total={filtered.length} />
    {/if}
  {/if}
</div>
