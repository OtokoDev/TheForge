<script>
  import { currentBusinessId } from '../lib/session.js'
  import JournalTimeline from '../components/JournalTimeline.svelte'
</script>

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut) pour voir la main courante.</p>
{:else}
  {#key $currentBusinessId}
    <JournalTimeline path={`/api/businesses/${$currentBusinessId}/activity`} title="Main courante" subtitle="Journal d'activité du business, filtrable par type et par jour." />
  {/key}
{/if}
