<script>
  import { me, currentBusinessId, currentBusiness } from '../lib/session.js'
  import { canAdminBusiness } from '../lib/roles.js'
  import SettingsLayout from '../components/layout/SettingsLayout.svelte'
  import LogoSection from '../components/business/LogoSection.svelte'
  import TaxSection from '../components/business/TaxSection.svelte'
  import AccountsSection from '../components/business/AccountsSection.svelte'
  import MembersSection from '../components/business/MembersSection.svelte'

  let canAdmin = $derived($currentBusinessId ? canAdminBusiness($me, $currentBusinessId) : false)
  const TABS = [
    { separator: 'Atelier' },
    { key: 'coffres', label: 'Coffres' },
    { key: 'taxe', label: 'Taxe' },
    { separator: 'Identité' },
    { key: 'logo', label: 'Logo' },
    { separator: 'Équipe' },
    { key: 'membres', label: 'Membres' },
  ]
  let tab = $state('coffres')
</script>

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut) à configurer.</p>
{:else if !canAdmin}
  <p class="text-sm text-destructive">Réservé à l'administrateur de ce business.</p>
{:else}
  {#key $currentBusinessId}
    <SettingsLayout
      title={`Configuration — ${$currentBusiness?.nom ?? ''}`}
      subtitle="Paramètres du business courant (admin)."
      tabs={TABS}
      active={tab}
      onSelect={(k) => (tab = k)}
    >
      {#if tab === 'coffres'}
        <AccountsSection businessId={$currentBusinessId} />
      {:else if tab === 'taxe'}
        <TaxSection businessId={$currentBusinessId} />
      {:else if tab === 'logo'}
        <LogoSection businessId={$currentBusinessId} />
      {:else if tab === 'membres'}
        <MembersSection businessId={$currentBusinessId} />
      {/if}
    </SettingsLayout>
  {/key}
{/if}
