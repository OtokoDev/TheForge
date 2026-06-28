<script>
  import { me } from '../lib/session.js'
  import { api, ApiError } from '../lib/api.js'
  import { GLOBAL_ROLE_LABELS } from '../lib/roles.js'
  import { formatDate } from '../lib/format.js'
  import { notifySuccess, notifyError } from '../lib/notifications.js'
  import PageHeader from '../components/PageHeader.svelte'
  import Card from '../components/ui/Card.svelte'
  import CardHeader from '../components/ui/CardHeader.svelte'
  import CardTitle from '../components/ui/CardTitle.svelte'
  import CardContent from '../components/ui/CardContent.svelte'
  import Input from '../components/ui/Input.svelte'
  import Button from '../components/ui/Button.svelte'
  import Badge from '../components/ui/Badge.svelte'
  import Checkbox from '../components/ui/Checkbox.svelte'

  const u = $me.user
  let inGameName = $state(u.inGameName ?? '')
  let savedName = $state(u.inGameName ?? '')
  let saving = $state(false)
  let webhooks = $state(u.webhooksEnabled)

  function fail(e) {
    notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  }

  // webhooks déjà basculé par bind:checked ; on persiste + maj le store me (UI globale à jour).
  async function persistWebhooks() {
    const next = webhooks
    try {
      await api('/api/me/webhooks', { method: 'PUT', body: JSON.stringify({ enabled: next }) })
      notifySuccess('Préférence enregistrée')
      me.update((m) => ({ ...m, user: { ...m.user, webhooksEnabled: next } }))
    } catch (e) {
      webhooks = !next
      fail(e)
    }
  }

  async function save() {
    saving = true
    try {
      await api('/api/me/in-game-name', { method: 'PUT', body: JSON.stringify({ inGameName }) })
      notifySuccess('Pseudo mis à jour')
      savedName = inGameName
      // Met à jour le store partagé → navbar/sidebar/dashboard reflètent le nouveau pseudo direct.
      me.update((m) => ({ ...m, user: { ...m.user, inGameName } }))
    } catch (e) {
      fail(e)
    } finally {
      saving = false
    }
  }
</script>

{#snippet row(label, value)}
  <div class="flex items-center justify-between border-b py-2 last:border-b-0">
    <span class="text-sm text-muted-foreground">{label}</span>
    <span class="text-sm font-medium">{value}</span>
  </div>
{/snippet}

<PageHeader title="Profil" description="Tes informations de compte." />

<Card>
  <CardHeader><CardTitle>Pseudo RP</CardTitle></CardHeader>
  <CardContent class="flex flex-wrap items-center gap-2">
    <Input class="max-w-xs" placeholder="Nom en jeu" bind:value={inGameName} />
    <Button onclick={save} disabled={saving || inGameName === savedName}>Enregistrer</Button>
  </CardContent>
</Card>

<Card>
  <CardHeader><CardTitle>Notifications</CardTitle></CardHeader>
  <CardContent>
    <Checkbox bind:checked={webhooks} onchange={persistWebhooks} label="Recevoir mes webhooks Discord (prise/fin de service, factures)" />
  </CardContent>
</Card>

<Card>
  <CardHeader><CardTitle>Compte</CardTitle></CardHeader>
  <CardContent>
    {@render row('Pseudo Discord', u.username)}
    {@render row('Nom en jeu', savedName || '—')}
    {@render row('Discord ID', u.discordId)}
    {@render row('Rôle global', GLOBAL_ROLE_LABELS[u.globalRole])}
    {@render row('Membre depuis', formatDate(u.createdAt))}
  </CardContent>
</Card>

<Card>
  <CardHeader><CardTitle>Appartenances</CardTitle></CardHeader>
  <CardContent class="flex flex-col gap-2">
    {#if $me.memberships.length === 0}
      <p class="text-sm text-muted-foreground">Aucune appartenance.</p>
    {:else}
      {#each $me.memberships as m (m.businessId)}
        <div class="flex items-center justify-between">
          <span class="text-sm">{m.businessNom}</span>
          <Badge variant={m.role === 'ADMIN' ? 'default' : 'outline'}>{m.role}</Badge>
        </div>
      {/each}
    {/if}
  </CardContent>
</Card>
