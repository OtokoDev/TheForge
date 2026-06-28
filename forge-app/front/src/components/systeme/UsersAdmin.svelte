<script>
  import { onMount } from 'svelte'
  import { Ban, RotateCcw } from '@lucide/svelte'
  import { api, ApiError } from '../../lib/api.js'
  import { me } from '../../lib/session.js'
  import { GLOBAL_ROLE_LABELS } from '../../lib/roles.js'
  import { notifySuccess, notifyError } from '../../lib/notifications.js'
  import Badge from '../ui/Badge.svelte'
  import Button from '../ui/Button.svelte'
  import Input from '../ui/Input.svelte'
  import SelectField from '../ui/SelectField.svelte'

  const ROLE_OPTIONS = [
    { value: 'NONE', label: 'Membre' },
    { value: 'STAFF', label: 'Staff' },
    { value: 'SYSTEM', label: 'Système' },
  ]

  let users = $state([])
  let query = $state('')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  async function load() {
    try {
      users = await api('/api/users/all')
    } catch (e) {
      fail(e)
    }
  }
  onMount(load)

  let filtered = $derived.by(() => {
    const q = query.trim().toLowerCase()
    return users.filter((u) => q === '' || u.username.toLowerCase().includes(q) || (u.inGameName ?? '').toLowerCase().includes(q))
  })

  async function changeRole(u, role) {
    try {
      await api(`/api/users/${u.id}/role`, { method: 'PUT', body: JSON.stringify({ role }) })
      notifySuccess('Rôle mis à jour')
      load()
    } catch (e) {
      fail(e)
    }
  }

  async function toggle(u) {
    const active = !u.active
    if (!active && !window.confirm(`Bannir ${u.username} ?\nSa session active sera fermée et l'API bloquée immédiatement.`)) return
    try {
      await api(`/api/users/${u.id}/active`, { method: 'PUT', body: JSON.stringify({ active }) })
      notifySuccess(active ? 'Compte réactivé' : 'Compte banni')
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<div class="flex flex-col gap-3">
  <p class="text-sm text-muted-foreground">
    Bannir désactive le compte : effet immédiat (session fermée + API bloquée), le token n'est plus accepté.
  </p>
  <Input class="max-w-xs" ariaLabel="Rechercher un utilisateur" placeholder="Rechercher un utilisateur…" bind:value={query} />

  <div class="overflow-auto rounded-md border">
    <table class="w-full text-sm">
      <thead class="bg-muted/50 text-left text-xs text-muted-foreground">
        <tr>
          <th class="px-3 py-2">Utilisateur</th>
          <th class="px-3 py-2">Rôle</th>
          <th class="px-3 py-2">Statut</th>
          <th class="px-3 py-2 text-right">Action</th>
        </tr>
      </thead>
      <tbody>
        {#each filtered as u (u.id)}
          <tr class="border-t">
            <td class="px-3 py-2">
              <div class="font-medium">{u.inGameName ?? u.username}</div>
              <div class="text-xs text-muted-foreground">@{u.username}</div>
            </td>
            <td class="px-3 py-2">
              {#if u.id === $me.user.id}
                <Badge variant="outline">{GLOBAL_ROLE_LABELS[u.globalRole]}</Badge>
              {:else}
                <SelectField value={u.globalRole} onChange={(v) => changeRole(u, v)} options={ROLE_OPTIONS} />
              {/if}
            </td>
            <td class="px-3 py-2">
              {#if u.active}<Badge variant="secondary">actif</Badge>{:else}<Badge variant="destructive">banni</Badge>{/if}
            </td>
            <td class="px-3 py-2 text-right">
              {#if u.id === $me.user.id}
                <span class="text-xs text-muted-foreground">vous</span>
              {:else if u.active}
                <Button variant="outline" size="sm" onclick={() => toggle(u)}><Ban size={14} /> Bannir</Button>
              {:else}
                <Button variant="outline" size="sm" onclick={() => toggle(u)}><RotateCcw size={14} /> Réactiver</Button>
              {/if}
            </td>
          </tr>
        {/each}
        {#if filtered.length === 0}<tr><td colspan="4" class="px-3 py-3 text-muted-foreground">Aucun utilisateur.</td></tr>{/if}
      </tbody>
    </table>
  </div>
</div>
