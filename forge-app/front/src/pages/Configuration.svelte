<script>
  import { me, currentBusinessId, currentBusiness } from '../lib/session.js'
  import { canAdminBusiness } from '../lib/roles.js'
  import PageHeader from '../components/PageHeader.svelte'
  import LogoSection from '../components/business/LogoSection.svelte'
  import TaxSection from '../components/business/TaxSection.svelte'
  import AccountsSection from '../components/business/AccountsSection.svelte'
  import DefaultsSection from '../components/business/DefaultsSection.svelte'
  import MembersSection from '../components/business/MembersSection.svelte'

  let canAdmin = $derived($currentBusinessId ? canAdminBusiness($me, $currentBusinessId) : false)
</script>

<PageHeader title="Configuration" description="Paramètres du business courant (admin)." />

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut) à configurer.</p>
{:else if !canAdmin}
  <p class="text-sm text-destructive">Réservé à l'administrateur de ce business.</p>
{:else}
  {#key $currentBusinessId}
    <div class="flex flex-col gap-6">
      <p class="text-sm text-muted-foreground">Configuration de <strong>{$currentBusiness?.nom}</strong>.</p>
      <LogoSection businessId={$currentBusinessId} />
      <TaxSection businessId={$currentBusinessId} />
      <AccountsSection businessId={$currentBusinessId} />
      <DefaultsSection businessId={$currentBusinessId} />
      <MembersSection businessId={$currentBusinessId} />
    </div>
  {/key}
{/if}
