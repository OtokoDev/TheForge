<script>
  import { me, currentBusinessId, currentBusiness } from '../lib/session.js'
  import { canAdminBusiness } from '../lib/roles.js'
  import SettingsLayout from '../components/layout/SettingsLayout.svelte'
  import LogoSection from '../components/business/LogoSection.svelte'
  import TaxSection from '../components/business/TaxSection.svelte'
  import AccountsSection from '../components/business/AccountsSection.svelte'
  import MembersSection from '../components/business/MembersSection.svelte'
  import DevSection from '../components/business/DevSection.svelte'
  import LogsSection from '../components/business/LogsSection.svelte'
  import MapTypesSection from '../components/business/MapTypesSection.svelte'
  import WebhookSection from '../components/business/WebhookSection.svelte'
  import Catalogue from './Catalogue.svelte'

  let canAdmin = $derived($currentBusinessId ? canAdminBusiness($me, $currentBusinessId) : false)
  let isSystem = $derived($me.user.globalRole === 'SYSTEM')
  let isCompagnie = $derived($currentBusiness?.type === 'COMPAGNIE')
  let TABS = $derived([
    { separator: 'Atelier' },
    { key: 'coffres', label: 'Coffres' },
    { key: 'taxe', label: 'Taxe' },
    { key: 'catalogue', label: 'Catalogue' },
    { separator: 'Identité' },
    { key: 'logo', label: 'Logo' },
    { separator: 'Équipe' },
    { key: 'membres', label: 'Membres' },
    { separator: 'Activité' },
    { key: 'logs', label: 'Logs' },
    { key: 'webhook', label: 'Webhook Discord' },
    ...(isCompagnie ? [{ separator: 'Carte' }, { key: 'carte', label: 'Marqueurs' }] : []),
    ...(isSystem ? [{ separator: 'Système' }, { key: 'systeme', label: 'Outils de test' }] : []),
  ])
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
      {:else if tab === 'catalogue'}
        <Catalogue />
      {:else if tab === 'logo'}
        <LogoSection businessId={$currentBusinessId} />
      {:else if tab === 'membres'}
        <MembersSection businessId={$currentBusinessId} />
      {:else if tab === 'logs'}
        <LogsSection businessId={$currentBusinessId} />
      {:else if tab === 'webhook'}
        <WebhookSection businessId={$currentBusinessId} />
      {:else if tab === 'carte'}
        <MapTypesSection businessId={$currentBusinessId} />
      {:else if tab === 'systeme'}
        <DevSection businessId={$currentBusinessId} />
      {/if}
    </SettingsLayout>
  {/key}
{/if}
