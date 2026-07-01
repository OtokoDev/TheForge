<script>
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError } from '../../lib/notifications.js'

  let { businessId } = $props()
  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  let summary = $state(null)
  $effect(() => {
    if (!businessId) return
    api(`/api/businesses/${businessId}/finance/summary`).then((v) => (summary = v)).catch(fail)
  })
</script>

<div class="flex flex-col gap-2">
  <div class="text-sm font-semibold">Compte de résultat</div>
  <div class="grid grid-cols-2 gap-3 md:grid-cols-4 lg:grid-cols-7">
    {@render kpi('Encaissé', summary?.caEncaisse)}
    {@render kpi('Coût matières', summary?.coutTotal)}
    {@render kpi('Bénéfice', summary?.benefice)}
    {@render kpi('Part entreprise', summary?.partBusiness)}
    {@render kpi('Paie versée', summary?.paieVersee)}
    {@render kpi('Dépenses', summary?.depenses)}
    {@render kpi('Résultat', summary?.resultat, true)}
  </div>
</div>

{#snippet kpi(label, value, accent = false)}
  <div class="rounded-xl border p-4" style={accent ? 'border-color:rgba(232,89,12,0.32);' : ''}>
    <div class="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</div>
    <div class="mt-1 text-xl font-bold tabular-nums" style={accent ? 'color:#f5a06a;' : ''}>{fmt(value)} <span class="text-xs font-normal text-muted-foreground">septims</span></div>
  </div>
{/snippet}
