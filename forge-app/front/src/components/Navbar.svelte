<script>
  import { Menu } from '@lucide/svelte'
  import { LOGOUT_URL } from '../lib/api.js'
  import { me, businesses, currentBusinessId, currentLogo, shift, setCurrentBusiness } from '../lib/session.js'
  import { GLOBAL_ROLE_LABELS } from '../lib/roles.js'
  import Badge from './ui/Badge.svelte'
  import Button from './ui/Button.svelte'
  import SelectField from './ui/SelectField.svelte'

  let { onMenu = () => {} } = $props()
  let user = $derived($me.user)
  let displayName = $derived(user.inGameName ?? user.username)
  let initials = $derived(displayName.slice(0, 2).toUpperCase())
</script>

<header class="flex min-h-16 items-center justify-between gap-3 border-b bg-background/75 px-4 backdrop-blur lg:px-8">
  <div class="flex items-center gap-3">
    <button
      onclick={onMenu}
      aria-label="Ouvrir le menu"
      class="flex size-9 shrink-0 items-center justify-center rounded-md border text-muted-foreground transition hover:bg-muted hover:text-foreground lg:hidden"
    >
      <Menu size={20} />
    </button>

    {#if $currentLogo}
      <img src={$currentLogo} alt="Logo du business" class="size-10 shrink-0 rounded-md border bg-muted object-contain" />
    {/if}

    {#if $businesses.length === 0}
      <span class="text-xs text-muted-foreground">Aucun business</span>
    {:else}
      <SelectField
        ariaLabel="Business courant"
        value={$currentBusinessId ?? ''}
        onChange={(v) => setCurrentBusiness(v)}
        options={$businesses.map((b) => ({ value: b.id, label: b.nom }))}
      />
    {/if}

    {#if $shift?.open}
      <span class="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/40 bg-emerald-500/15 px-2.5 py-0.5 text-xs font-medium text-emerald-400">
        <span class="size-1.5 animate-pulse rounded-full bg-emerald-400"></span>
        Service en cours
      </span>
    {/if}
  </div>

  <div class="flex items-center gap-3">
    <Badge variant="outline">{GLOBAL_ROLE_LABELS[user.globalRole]}</Badge>
    <div class="flex size-9 items-center justify-center overflow-hidden rounded-full bg-muted text-sm font-medium">
      {#if user.avatar}
        <img src={user.avatar} alt={user.username} class="size-full object-cover" />
      {:else}
        {initials}
      {/if}
    </div>
    <div class="hidden text-right sm:block">
      <p class="text-sm font-medium">{displayName}</p>
      <p class="text-xs text-muted-foreground">@{user.username}</p>
    </div>
    <Button variant="outline" size="sm" onclick={() => (window.location.href = LOGOUT_URL)}>
      Déconnexion
    </Button>
  </div>
</header>
