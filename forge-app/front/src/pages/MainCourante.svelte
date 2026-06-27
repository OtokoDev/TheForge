<script>
  import { me, currentBusinessId } from '../lib/session.js'
  import { api, ApiError } from '../lib/api.js'
  import { notifyError } from '../lib/notifications.js'
  import Badge from '../components/ui/Badge.svelte'
  import Button from '../components/ui/Button.svelte'

  const isSystem = $me.user.globalRole === 'SYSTEM'
  let scope = $state('business')
  let limit = $state(200)
  let rows = $state([])

  const LABELS = {
    LOGIN_OK: 'Connexion',
    LOGIN_FAIL: 'Connexion refusée',
    MEMBER_ADD: 'Ajout membre',
    ROLE_SET: 'Changement de rôle',
    CREANCE_DEPOT: 'Dépôt créance',
    CREANCE_PAIEMENT: 'Paiement créance',
  }
  const label = (a) => LABELS[a] ?? a
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  $effect(() => {
    const s = scope
    const l = limit
    const id = $currentBusinessId
    const url = s === 'system'
      ? `/api/system/activity?limit=${l}`
      : id ? `/api/businesses/${id}/activity?limit=${l}` : null
    if (!url) {
      rows = []
      return
    }
    let active = true
    api(url).then((r) => active && (rows = r)).catch(fail)
    return () => { active = false }
  })

  let byDay = $derived.by(() => {
    const map = new Map()
    for (const e of rows) {
      const d = new Date(e.createdAt).toLocaleDateString('fr-FR', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' })
      const bucket = map.get(d) ?? []
      bucket.push(e)
      map.set(d, bucket)
    }
    return Array.from(map)
  })

  const time = (v) => new Date(v).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
</script>

<div class="flex flex-col gap-4">
  <div class="flex flex-wrap items-end justify-between gap-3">
    <div>
      <h1 class="text-2xl font-bold tracking-tight">Main courante</h1>
      <p class="mt-1 text-sm text-muted-foreground">Journal d'activité, regroupé par jour.</p>
    </div>
    {#if isSystem}
      <div class="flex gap-1 rounded-md border p-1">
        <Button variant={scope === 'business' ? 'default' : 'ghost'} size="sm" onclick={() => { scope = 'business'; limit = 200 }}>Business</Button>
        <Button variant={scope === 'system' ? 'default' : 'ghost'} size="sm" onclick={() => { scope = 'system'; limit = 200 }}>Système</Button>
      </div>
    {/if}
  </div>

  {#if scope === 'business' && !$currentBusinessId}
    <p class="text-sm text-muted-foreground">Sélectionne un business (en haut).</p>
  {/if}

  {#each byDay as [day, items] (day)}
    <div class="rounded-md border">
      <div class="sticky top-0 border-b bg-card/95 px-4 py-2 text-sm font-semibold capitalize backdrop-blur">{day}</div>
      <div class="flex flex-col divide-y">
        {#each items as e, i (i)}
          <div class="flex flex-wrap items-center gap-3 px-4 py-2 text-sm">
            <span class="w-12 shrink-0 tabular-nums text-muted-foreground">{time(e.createdAt)}</span>
            <Badge variant="outline">{label(e.action)}</Badge>
            <span class="min-w-0 flex-1 truncate">{e.details ?? ''}</span>
            <span class="shrink-0 text-muted-foreground">{e.username}</span>
          </div>
        {/each}
      </div>
    </div>
  {/each}

  {#if rows.length === 0}
    <p class="text-sm text-muted-foreground">Aucune activité.</p>
  {/if}
  {#if rows.length >= limit}
    <Button variant="outline" class="self-center" onclick={() => (limit += 200)}>Charger plus</Button>
  {/if}
</div>
