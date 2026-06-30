<script>
  import JournalTimeline from '../JournalTimeline.svelte'

  let { businessId } = $props()
</script>

{#key businessId}
  <JournalTimeline
    path={`/api/businesses/${businessId}/activity`}
    title="Logs"
    subtitle="Journal d'activité du business, filtrable par type et par jour."
  />
{/key}
