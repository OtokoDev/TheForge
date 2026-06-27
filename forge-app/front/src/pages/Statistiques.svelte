<script>
  import { currentBusinessId } from '../lib/session.js'
  import { api, ApiError } from '../lib/api.js'
  import { exportCsv } from '../lib/csv.js'
  import { notifyError } from '../lib/notifications.js'
  import BarsV from '../components/charts/BarsV.svelte'
  import BarsH from '../components/charts/BarsH.svelte'
  import Area from '../components/charts/Area.svelte'
  import Donut from '../components/charts/Donut.svelte'

  const ORANGE = '#E8590C'
  const GREEN = '#5fa890'
  const RED = '#ed8472'
  const fmt = (n) => Number(n ?? 0).toLocaleString('fr-FR')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  const TABS = [
    { key: 'overview', label: "Vue d'ensemble" },
    { key: 'products', label: 'Produits' },
    { key: 'forgerons', label: 'Forgerons' },
    { key: 'stock', label: 'Stock' },
    { key: 'activity', label: 'Activité' },
    { key: 'creances', label: 'Créances' },
    { key: 'clients', label: 'Clients' },
  ]
  const PERIODS = [{ d: 7, l: '7 j' }, { d: 30, l: '30 j' }, { d: 90, l: '90 j' }]
  const DOW = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim']
  const iso = (d) => d.toISOString().slice(0, 10)

  let tab = $state('overview')
  let from = $state(new Date(Date.now() - 30 * 86400000))
  let to = $state(new Date())
  let data = $state(null)
  let loading = $state(false)
  let metric = $state('ca')

  function preset(d) {
    from = new Date(Date.now() - d * 86400000)
    to = new Date()
  }
  function month() {
    const n = new Date()
    from = new Date(n.getFullYear(), n.getMonth(), 1)
    to = new Date()
  }

  $effect(() => {
    const id = $currentBusinessId
    const t = tab
    const f = from.toISOString()
    const tt = to.toISOString()
    if (!id) return
    loading = true
    data = null
    let active = true
    api(`/api/businesses/${id}/stats/${t}?from=${f}&to=${tt}`)
      .then((v) => active && (data = v))
      .catch(fail)
      .finally(() => active && (loading = false))
    return () => { active = false }
  })

  let topProducts = $derived.by(() => {
    if (!data?.top) return []
    return [...data.top].sort((a, b) => b[metric] - a[metric]).slice(0, 10)
  })
  let heatMap = $derived.by(() => {
    const m = new Map()
    ;(data?.heatmap ?? []).forEach((c) => m.set(`${c.dow}-${c.hour}`, c))
    return m
  })
  let heatMax = $derived(Math.max(1, ...(data?.heatmap ?? []).map((c) => c.ca)))
</script>

{#snippet kpi(label, value, sub, cur, prev, color)}
  {@const delta = cur !== undefined && prev !== undefined ? (prev > 0 ? ((cur - prev) / prev) * 100 : cur > 0 ? 100 : 0) : null}
  <div class="rounded-md border bg-card p-3">
    <div class="text-xs uppercase tracking-wide text-muted-foreground">{label}</div>
    <div class="mt-1 text-xl font-bold" style={color ? `color:${color}` : ''}>{value}</div>
    {#if sub}<div class="text-xs text-muted-foreground">{sub}</div>{/if}
    {#if delta !== null}
      <div class="text-xs font-medium {delta >= 0 ? 'text-emerald-500' : 'text-red-400'}">{delta >= 0 ? '▲' : '▼'} {Math.abs(delta).toFixed(0)} %</div>
    {/if}
  </div>
{/snippet}

{#snippet csvBtn(onClick)}
  <button onclick={onClick} class="rounded border px-2 py-1 text-xs text-muted-foreground hover:bg-muted">Export CSV</button>
{/snippet}

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut) pour voir les statistiques.</p>
{:else}
  <div class="flex flex-col gap-4">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-bold tracking-tight">Statistiques</h1>
      <div class="flex flex-wrap items-center gap-1">
        {#each PERIODS as p (p.d)}
          <button onclick={() => preset(p.d)} class="rounded-md px-3 py-1.5 text-sm font-medium text-muted-foreground transition hover:bg-muted">{p.l}</button>
        {/each}
        <button onclick={month} class="rounded-md px-3 py-1.5 text-sm font-medium text-muted-foreground transition hover:bg-muted">Mois</button>
        <input type="date" value={iso(from)} max={iso(to)} onchange={(e) => e.currentTarget.value && (from = new Date(e.currentTarget.value))} class="rounded-md border bg-card px-2 py-1 text-sm" />
        <span class="text-muted-foreground">→</span>
        <input type="date" value={iso(to)} min={iso(from)} onchange={(e) => e.currentTarget.value && (to = new Date(e.currentTarget.value + 'T23:59:59'))} class="rounded-md border bg-card px-2 py-1 text-sm" />
      </div>
    </div>

    <div class="flex flex-wrap gap-1 border-b">
      {#each TABS as t (t.key)}
        <button onclick={() => (tab = t.key)} class="-mb-px border-b-2 px-3 py-2 text-sm font-medium transition {tab === t.key ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground'}">{t.label}</button>
      {/each}
    </div>

    {#if loading}
      <p class="text-sm text-muted-foreground">Chargement…</p>
    {:else if data}
      {#if tab === 'overview'}
        <div class="flex flex-col gap-4">
          <div class="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-6">
            {@render kpi('Encaissé', fmt(data.caEncaisse), undefined, data.caEncaisse, data.caEncaissePrev)}
            {@render kpi('Bénéfice', fmt(data.benefice), undefined, data.benefice, data.beneficePrev)}
            {@render kpi('Taux marge', `${(data.tauxMarge * 100).toFixed(1)} %`)}
            {@render kpi('Panier moyen', fmt(data.panierMoyen), undefined, data.panierMoyen, data.panierMoyenPrev)}
            {@render kpi('Factures', String(data.nbFactures), undefined, data.nbFactures, data.nbFacturesPrev)}
            {@render kpi('Impayé', fmt(data.impaye), `${data.impayeCount} fact.`, undefined, undefined, RED)}
          </div>
          <div class="rounded-md border bg-card p-3">
            <div class="mb-2 text-sm font-semibold">CA &amp; bénéfice par jour</div>
            <Area data={data.serie} x="jour" series={[{ key: 'ca', label: 'CA', color: ORANGE }, { key: 'benefice', label: 'Bénéfice', color: GREEN }]} format={fmt} />
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            <div class="rounded-md border bg-card p-3">
              <div class="mb-2 text-sm font-semibold">Encaissé vs à crédit</div>
              <Donut data={[{ nom: 'Encaissé', valeur: data.caEncaisse }, { nom: 'À crédit', valeur: data.impaye }]} colors={[GREEN, RED]} format={fmt} />
            </div>
            <div class="rounded-md border bg-card p-3">
              <div class="mb-2 text-sm font-semibold">Répartition bénéfice</div>
              <Donut data={[{ nom: 'Part business', valeur: data.partBusiness }, { nom: 'Part forgeron', valeur: data.partForgeron }]} colors={[ORANGE, '#a288bd']} format={fmt} />
            </div>
          </div>
        </div>
      {:else if tab === 'products'}
        <div class="flex flex-col gap-4">
          <div class="rounded-md border bg-card p-3">
            <div class="mb-2 flex items-center justify-between">
              <div class="text-sm font-semibold">Top produits</div>
              <div class="flex items-center gap-1">
                {#each ['ca', 'marge', 'qte'] as m (m)}
                  <button onclick={() => (metric = m)} class="rounded px-2 py-1 text-xs font-medium {metric === m ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted'}">{m === 'ca' ? 'CA' : m === 'marge' ? 'Marge' : 'Qté'}</button>
                {/each}
                {@render csvBtn(() => exportCsv('top-produits', ['Produit', 'CA', 'Marge', 'Qté'], data.top.map((p) => [p.name, p.ca, p.marge, p.qte])))}
              </div>
            </div>
            <BarsH data={topProducts} label="name" value={metric} color={ORANGE} format={fmt} />
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            <div class="rounded-md border bg-card p-3"><div class="mb-2 text-sm font-semibold">Ventes par famille</div><Donut data={data.parFamille} format={fmt} /></div>
            <div class="rounded-md border bg-card p-3"><div class="mb-2 text-sm font-semibold">Ventes par matériau</div><Donut data={data.parMateriau} format={fmt} /></div>
          </div>
          <div class="rounded-md border">
            <div class="border-b px-4 py-2 text-sm font-semibold">⚠ Vendus à perte ({data.pertes.length})</div>
            <div class="flex flex-col divide-y">
              {#if data.pertes.length === 0}<p class="px-4 py-3 text-sm text-muted-foreground">Aucun produit vendu à perte.</p>{/if}
              {#each data.pertes as p, i (i)}
                <div class="flex items-center justify-between px-4 py-2 text-sm">
                  <span>{p.name}</span>
                  <span class="text-muted-foreground">revente <strong class="text-foreground">{fmt(p.prixRevente)}</strong> &lt; coût <strong style="color:{RED}">{fmt(p.cout)}</strong></span>
                </div>
              {/each}
            </div>
          </div>
        </div>
      {:else if tab === 'forgerons'}
        <div class="flex flex-col gap-4">
          <div class="flex justify-end">
            {@render csvBtn(() => exportCsv('forgerons', ['Forgeron', 'CA', 'Bénéfice', 'Factures', 'Heures', 'CA/h'], data.forgerons.map((f) => [f.username, f.ca, f.benefice, f.nbFactures, (f.minutesService / 60).toFixed(1), Math.round(f.caParHeure)])))}
          </div>
          <div class="overflow-auto rounded-md border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50 text-left text-xs text-muted-foreground">
                <tr><th class="px-3 py-2">Forgeron</th><th class="px-3 py-2 text-right">CA</th><th class="px-3 py-2 text-right">Bénéfice</th><th class="px-3 py-2 text-right">Factures</th><th class="px-3 py-2 text-right">Heures</th><th class="px-3 py-2 text-right">CA / h</th></tr>
              </thead>
              <tbody>
                {#each data.forgerons as f (f.userId)}
                  <tr class="border-t">
                    <td class="px-3 py-2 font-medium">{f.username}</td>
                    <td class="px-3 py-2 text-right tabular-nums">{fmt(f.ca)}</td>
                    <td class="px-3 py-2 text-right tabular-nums">{fmt(f.benefice)}</td>
                    <td class="px-3 py-2 text-right tabular-nums">{f.nbFactures}</td>
                    <td class="px-3 py-2 text-right tabular-nums">{(f.minutesService / 60).toFixed(1)}</td>
                    <td class="px-3 py-2 text-right tabular-nums">{fmt(Math.round(f.caParHeure))}</td>
                  </tr>
                {/each}
                {#if data.forgerons.length === 0}<tr><td colspan="6" class="px-3 py-3 text-muted-foreground">Aucune activité.</td></tr>{/if}
              </tbody>
            </table>
          </div>
          <div class="rounded-md border bg-card p-3">
            <div class="mb-2 text-sm font-semibold">CA par heure de service</div>
            <BarsV data={data.forgerons.map((f) => ({ name: f.username, caParHeure: Math.round(f.caParHeure) }))} x="name" series={[{ key: 'caParHeure', label: 'CA/h', color: GREEN }]} format={fmt} />
          </div>
        </div>
      {:else if tab === 'stock'}
        <div class="flex flex-col gap-4">
          <div class="grid gap-3 sm:grid-cols-2">
            {@render kpi('Valeur du stock (coût)', `${fmt(data.valeurStock)} septims`)}
            {@render kpi('Références en rupture/faible', String(data.ruptures.length), undefined, undefined, undefined, data.ruptures.length ? RED : undefined)}
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            <div class="rounded-md border">
              <div class="border-b px-4 py-2 text-sm font-semibold">Stock faible (≤ 5)</div>
              <div class="flex flex-col divide-y">
                {#if data.ruptures.length === 0}<p class="px-4 py-3 text-sm text-muted-foreground">Rien en rupture.</p>{/if}
                {#each data.ruptures as r, i (i)}
                  <div class="flex items-center justify-between px-4 py-2 text-sm"><span>{r.nom}</span><span style={r.valeur <= 0 ? `color:${RED}` : ''}>{r.valeur}</span></div>
                {/each}
              </div>
            </div>
            <div class="rounded-md border bg-card p-3">
              <div class="mb-2 text-sm font-semibold">Top matières consommées</div>
              <BarsH data={data.topConsommees} label="nom" value="valeur" color={ORANGE} format={fmt} />
            </div>
          </div>
        </div>
      {:else if tab === 'activity'}
        <div class="flex flex-col gap-4">
          <div class="grid gap-3 sm:grid-cols-3">
            {@render kpi('Sessions', String(data.sessions))}
            {@render kpi('Durée moyenne', `${(data.dureeMoyenneMin / 60).toFixed(1)} h`)}
            {@render kpi('CA / session', fmt(data.caParSession))}
          </div>
          <div class="overflow-auto rounded-md border bg-card p-3">
            <div class="mb-2 text-sm font-semibold">Quand vend-on ? (CA par jour × heure)</div>
            <div class="inline-block">
              <div class="flex">
                <div class="w-10"></div>
                {#each Array(24) as _, h (h)}<div class="w-6 text-center text-[9px] text-muted-foreground">{h}</div>{/each}
              </div>
              {#each DOW as day, dow (dow)}
                <div class="flex items-center">
                  <div class="w-10 text-xs text-muted-foreground">{day}</div>
                  {#each Array(24) as _, h (h)}
                    {@const c = heatMap.get(`${dow}-${h}`)}
                    {@const v = c ? c.ca : 0}
                    <div title={c ? `${day} ${h}h — ${fmt(c.ca)} (${c.count} fact.)` : ''} class="m-px h-6 w-6 rounded-sm" style="background:{v ? `rgba(232,89,12,${0.15 + (0.85 * v) / heatMax})` : 'rgba(255,255,255,0.04)'}"></div>
                  {/each}
                </div>
              {/each}
            </div>
          </div>
        </div>
      {:else if tab === 'creances'}
        <div class="flex flex-col gap-4">
          <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
            {@render kpi('Total dû', fmt(data.totalDu), undefined, undefined, undefined, data.totalDu ? RED : undefined)}
            {@render kpi('Crédité', fmt(data.totalCredit))}
            {@render kpi('Payé', fmt(data.totalPaid))}
            {@render kpi('Ratio payé', `${(data.ratioPaye * 100).toFixed(0)} %`)}
          </div>
          <div class="flex justify-end">
            {@render csvBtn(() => exportCsv('creances-farmeurs', ['Farmeur', 'Crédité', 'Payé', 'Reste dû'], data.topFarmers.map((f) => [f.username, f.credited, f.paid, f.remaining])))}
          </div>
          <div class="rounded-md border bg-card p-3">
            <div class="mb-2 text-sm font-semibold">Crédits vs paiements par jour</div>
            <Area data={data.serie} x="jour" series={[{ key: 'credit', label: 'Crédit', color: ORANGE }, { key: 'paiement', label: 'Paiement', color: GREEN }]} format={fmt} />
          </div>
          <div class="overflow-auto rounded-md border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50 text-left text-xs text-muted-foreground">
                <tr><th class="px-3 py-2">Farmeur</th><th class="px-3 py-2 text-right">Crédité</th><th class="px-3 py-2 text-right">Payé</th><th class="px-3 py-2 text-right">Reste dû</th></tr>
              </thead>
              <tbody>
                {#each data.topFarmers as f, i (i)}
                  <tr class="border-t">
                    <td class="px-3 py-2 font-medium">{f.username}</td>
                    <td class="px-3 py-2 text-right tabular-nums">{fmt(f.credited)}</td>
                    <td class="px-3 py-2 text-right tabular-nums">{fmt(f.paid)}</td>
                    <td class="px-3 py-2 text-right tabular-nums" style={f.remaining ? `color:${RED}` : ''}>{fmt(f.remaining)}</td>
                  </tr>
                {/each}
                {#if data.topFarmers.length === 0}<tr><td colspan="4" class="px-3 py-3 text-muted-foreground">Aucune créance.</td></tr>{/if}
              </tbody>
            </table>
          </div>
        </div>
      {:else}
        <div class="flex flex-col gap-4">
          <div class="rounded-md border bg-card p-3">
            <div class="mb-2 flex items-center justify-between">
              <div class="text-sm font-semibold">Top clients (CA)</div>
              {@render csvBtn(() => exportCsv('top-clients', ['Client', 'CA', 'Factures', 'Impayé'], data.top.map((c) => [c.nom, c.ca, c.nbFactures, c.impaye])))}
            </div>
            <BarsH data={data.top.map((c) => ({ name: c.nom, ca: c.ca }))} label="name" value="ca" color={ORANGE} format={fmt} />
          </div>
          <div class="rounded-md border">
            <div class="flex items-center justify-between border-b px-4 py-2">
              <span class="text-sm font-semibold">Débiteurs ({data.debiteurs.length})</span>
              {@render csvBtn(() => exportCsv('debiteurs', ['Client', 'Impayé', 'Factures'], data.debiteurs.map((c) => [c.nom, c.impaye, c.nbFactures])))}
            </div>
            <div class="flex flex-col divide-y">
              {#if data.debiteurs.length === 0}<p class="px-4 py-3 text-sm text-muted-foreground">Aucun impayé.</p>{/if}
              {#each data.debiteurs as c, i (i)}
                <div class="flex items-center justify-between px-4 py-2 text-sm">
                  <span>{c.nom} <span class="text-muted-foreground">· {c.nbFactures} fact.</span></span>
                  <span style="color:{RED}">{fmt(c.impaye)}</span>
                </div>
              {/each}
            </div>
          </div>
        </div>
      {/if}
    {/if}
  </div>
{/if}
