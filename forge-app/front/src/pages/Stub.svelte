<script>
  import { router } from 'svelte-spa-router'
  import PageHeader from '../components/PageHeader.svelte'
</script>

<PageHeader title="Écran en cours de migration" description={`Route ${router.location} — à porter (phase P2).`} />

<div class="rounded-lg border border-dashed bg-card/50 p-8 text-center text-sm text-muted-foreground">
  Cette page sera portée en Svelte lors de la phase P2 de la migration.
</div>
