<script>
  import { Trash2 } from '@lucide/svelte'
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess, toast } from '../../lib/notifications.js'
  import UserAutocomplete from '../admin/UserAutocomplete.svelte'
  import Card from '../ui/Card.svelte'
  import CardHeader from '../ui/CardHeader.svelte'
  import CardTitle from '../ui/CardTitle.svelte'
  import CardContent from '../ui/CardContent.svelte'
  import Button from '../ui/Button.svelte'
  import SelectField from '../ui/SelectField.svelte'

  let { businessId } = $props()
  let members = $state(null)
  let picked = $state(null)
  let acKey = $state(0)
  let role = $state('MEMBRE')

  const ROLE_OPTS = [{ value: 'MEMBRE', label: 'MEMBRE' }, { value: 'ADMIN', label: 'ADMIN' }]
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
  // 409 = données modifiées entre-temps → toast + refetch isolé (pas de F5).
  const onStale = (e) => {
    if (e instanceof ApiError && e.status === 409) {
      toast(e.message, 'info')
      load()
    } else fail(e)
  }

  function load() {
    api(`/api/businesses/${businessId}/members`).then((v) => (members = v)).catch(fail)
  }
  $effect(() => {
    businessId
    load()
  })

  async function add() {
    if (!picked) return notifyError('Choisis un utilisateur')
    try {
      await api(`/api/businesses/${businessId}/members`, { method: 'POST', body: JSON.stringify({ userId: picked.id, role, version: 0 }) })
      picked = null
      acKey += 1
      notifySuccess('Membre ajouté')
      load()
    } catch (e) {
      onStale(e)
    }
  }

  async function changeRole(userId, newRole, version) {
    try {
      await api(`/api/businesses/${businessId}/members`, { method: 'POST', body: JSON.stringify({ userId, role: newRole, version }) })
      load()
    } catch (e) {
      onStale(e)
    }
  }

  async function remove(userId) {
    try {
      await api(`/api/businesses/${businessId}/members/${userId}`, { method: 'DELETE' })
      notifySuccess('Membre retiré')
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<Card>
  <CardHeader><CardTitle>Membres</CardTitle></CardHeader>
  <CardContent class="flex flex-col gap-3">
    <div class="flex flex-wrap items-center gap-2 border-b pb-3">
      {#key acKey}
        <UserAutocomplete onSelect={(u) => (picked = u)} />
      {/key}
      <SelectField value={role} onChange={(v) => (role = v)} options={ROLE_OPTS} />
      <Button onclick={add}>Recruter</Button>
    </div>

    {#if members === null}
      <p class="text-sm text-muted-foreground">Chargement…</p>
    {:else if members.length === 0}
      <p class="text-sm text-muted-foreground">Aucun membre.</p>
    {:else}
      {#each members as m (m.userId)}
        <div class="flex flex-wrap items-center justify-between gap-2">
          <span class="text-sm">
            {#if m.inGameName}{m.inGameName} <span class="text-muted-foreground">(@{m.username})</span>{:else}@{m.username}{/if}
          </span>
          <div class="flex items-center gap-2">
            <SelectField value={m.role} onChange={(v) => changeRole(m.userId, v, m.version)} options={ROLE_OPTS} />
            <Button variant="ghost" size="icon" ariaLabel="Retirer le membre" onclick={() => remove(m.userId)}><Trash2 size={16} /></Button>
          </div>
        </div>
      {/each}
    {/if}
  </CardContent>
</Card>
