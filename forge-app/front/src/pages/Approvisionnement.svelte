<script>
  import { currentBusinessId, currentBusiness } from '../lib/session.js'
  import PageHeader from '../components/PageHeader.svelte'
  import Achats from './Achats.svelte'
  import Rachat from './Rachat.svelte'

  let isCompagnie = $derived($currentBusiness?.type === 'COMPAGNIE')
  let tab = $state('fournisseur')
  // Onglet Farmeurs (créances) réservé aux compagnies : repli si business = forge.
  $effect(() => {
    if (!isCompagnie && tab === 'farmeurs') tab = 'fournisseur'
  })
  let tabs = $derived([
    { key: 'fournisseur', label: 'Fournisseur' },
    ...(isCompagnie ? [{ key: 'farmeurs', label: 'Farmeurs' }] : []),
  ])
</script>

<PageHeader title="Approvisionnement" description="Matières entrantes — fournisseurs externes et rachat aux farmeurs." />

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut).</p>
{:else}
  <div class="mb-4 flex gap-1 border-b">
    {#each tabs as t (t.key)}
      <button
        onclick={() => (tab = t.key)}
        class="-mb-px border-b-2 px-4 py-2 text-sm font-medium transition {tab === t.key
          ? 'border-primary text-foreground'
          : 'border-transparent text-muted-foreground hover:text-foreground'}"
      >
        {t.label}
      </button>
    {/each}
  </div>

  {#if tab === 'fournisseur'}
    <Achats />
  {:else if tab === 'farmeurs'}
    <Rachat />
  {/if}
{/if}
