<script>
  import { me } from '../lib/session.js'
  import { api, ApiError } from '../lib/api.js'
  import { exportCsv } from '../lib/csv.js'
  import { canStaffView } from '../lib/roles.js'
  import { notifyError } from '../lib/notifications.js'
  import BarsV from '../components/charts/BarsV.svelte'
  import BarsH from '../components/charts/BarsH.svelte'

  const ORANGE = '#E8590C'
  const GREEN = '#5fa890'
  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const PERIODS = [{ w: 4, l: '4 sem.' }, { w: 12, l: '12 sem.' }, { w: 26, l: '26 sem.' }]

  const allowed = canStaffView($me)
  let weeks = $state(12)
  let d = $state(null)

  $effect(() => {
    if (!allowed) return
    const from = new Date(Date.now() - weeks * 7 * 86400000).toISOString()
    const to = new Date().toISOString()
    let active = true
    api(`/api/staff/stats/overview?from=${from}&to=${to}`)
      .then((v) => active && (d = v))
      .catch((e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue'))
    return () => { active = false }
  })
</script>

{#snippet kpi(label, value, sub)}
  <div class="rounded-md border bg-card p-3">
    <div class="text-xs uppercase tracking-wide text-muted-foreground">{label}</div>
    <div class="mt-1 truncate text-xl font-bold">{value}</div>
    {#if sub}<div class="text-xs text-muted-foreground">{sub}</div>{/if}
  </div>
{/snippet}

{#if !allowed}
  <p class="text-sm text-destructive">Réservé aux rôles STAFF / SYSTEM.</p>
{:else}
  <div class="flex flex-col gap-4">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-bold tracking-tight">Vue staff — global</h1>
        <p class="text-sm text-muted-foreground">CA de l'ensemble des business (lecture seule).</p>
      </div>
      <div class="flex gap-1">
        {#each PERIODS as p (p.w)}
          <button
            onclick={() => (weeks = p.w)}
            class="rounded-md px-3 py-1.5 text-sm font-medium transition {weeks === p.w ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted'}"
          >
            {p.l}
          </button>
        {/each}
      </div>
    </div>

    {#if !d}
      <p class="text-sm text-muted-foreground">Chargement…</p>
    {:else}
      <div class="grid grid-cols-2 gap-3 lg:grid-cols-3">
        {@render kpi('CA total', `${fmt(d.totalCa)} septims`)}
        {@render kpi('Bénéfice total', fmt(d.totalBenefice))}
        {@render kpi('Item le plus vendu', d.topItems[0]?.nom ?? '—', d.topItems[0] ? `${fmt(d.topItems[0].valeur)} u.` : undefined)}
      </div>

      <div class="rounded-md border bg-card p-3">
        <div class="mb-2 flex items-center justify-between">
          <div class="text-sm font-semibold">CA par semaine</div>
          <button class="rounded border px-2 py-0.5 text-xs text-muted-foreground hover:bg-muted"
            onclick={() => exportCsv('ca-hebdo', ['Semaine', 'CA', 'Bénéfice'], d.serie.map((s) => [s.semaine, s.ca, s.benefice]))}>Export CSV</button>
        </div>
        <BarsV data={d.serie} x="semaine" series={[{ key: 'ca', label: 'CA', color: ORANGE }, { key: 'benefice', label: 'Bénéfice', color: GREEN }]} format={fmt} />
      </div>

      <div class="grid gap-4 md:grid-cols-2">
        <div class="rounded-md border bg-card p-3">
          <div class="mb-2 flex items-center justify-between">
            <div class="text-sm font-semibold">CA par business</div>
            <button class="rounded border px-2 py-0.5 text-xs text-muted-foreground hover:bg-muted"
              onclick={() => exportCsv('ca-par-business', ['Business', 'CA'], d.parBusiness.map((b) => [b.nom, b.valeur]))}>Export CSV</button>
          </div>
          <BarsH data={d.parBusiness} label="nom" value="valeur" color={ORANGE} format={fmt} />
        </div>
        <div class="rounded-md border bg-card p-3">
          <div class="mb-2 flex items-center justify-between">
            <div class="text-sm font-semibold">Items les plus vendus (toutes forges)</div>
            <button class="rounded border px-2 py-0.5 text-xs text-muted-foreground hover:bg-muted"
              onclick={() => exportCsv('top-items', ['Item', 'Quantité'], d.topItems.map((t) => [t.nom, t.valeur]))}>Export CSV</button>
          </div>
          <BarsH data={d.topItems.slice(0, 10)} label="nom" value="valeur" color={GREEN} format={fmt} />
        </div>
      </div>
    {/if}
  </div>
{/if}
