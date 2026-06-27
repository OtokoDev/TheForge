<script>
  import { me } from '../lib/session.js'
  import PageHeader from '../components/PageHeader.svelte'
  import Badge from '../components/ui/Badge.svelte'

  let displayName = $derived($me.user.inGameName ?? $me.user.username)
</script>

<PageHeader title={`Bonjour, ${displayName}`} description="Vue d'ensemble de tes business." />

<div class="rounded-lg border bg-card text-card-foreground shadow-sm">
  <div class="border-b px-5 py-4">
    <h2 class="font-semibold">Mes business</h2>
  </div>
  <div class="flex flex-col gap-3 p-5">
    {#if $me.memberships.length === 0}
      <p class="text-sm text-muted-foreground">
        Tu n'appartiens à aucun business pour l'instant. Demande à un administrateur de t'ajouter.
      </p>
    {:else}
      {#each $me.memberships as m (m.businessId)}
        <div class="flex items-center justify-between rounded-md border px-4 py-3">
          <div>
            <p class="font-medium">{m.businessNom}</p>
            <p class="text-xs text-muted-foreground">{m.businessType}</p>
          </div>
          <Badge variant={m.role === 'ADMIN' ? 'default' : 'outline'}>{m.role}</Badge>
        </div>
      {/each}
    {/if}
  </div>
</div>
