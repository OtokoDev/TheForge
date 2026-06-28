<script>
  import { onMount } from 'svelte'
  import { me, currentBusinessId } from '../lib/session.js'
  import { api, ApiError } from '../lib/api.js'
  import { notifyError, notifySuccess, toast } from '../lib/notifications.js'
  import SettingsLayout from '../components/layout/SettingsLayout.svelte'
  import TaxonBoard from '../components/systeme/TaxonBoard.svelte'
  import BaseItemsBoard from '../components/systeme/BaseItemsBoard.svelte'
  import BusinessAdmin from '../components/systeme/BusinessAdmin.svelte'
  import Button from '../components/ui/Button.svelte'

  const isSystem = $me.user.globalRole === 'SYSTEM'
  const TABS = [
    { separator: 'Catalogue' },
    { key: 'familles', label: 'Familles' },
    { key: 'materiaux', label: 'Matériaux' },
    { key: 'objets', label: 'Objets de base' },
    { key: 'import', label: 'Import Skyrim' },
    { separator: 'Organisation' },
    { key: 'business', label: 'Business' },
  ]
  let tab = $state('familles')
  let families = $state([])
  let materials = $state([])
  let items = $state([])

  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function loadTaxa() {
    api('/api/catalog/families').then((v) => (families = v)).catch(fail)
    api('/api/catalog/materials').then((v) => (materials = v)).catch(fail)
  }
  function loadItems() {
    api('/api/catalog/items').then((v) => (items = v)).catch(fail)
  }
  onMount(() => {
    loadTaxa()
    loadItems()
  })

  async function runSeed() {
    const withPrices = !!$currentBusinessId
    if (!window.confirm(`Importer le catalogue Skyrim ?\nFamilles, matériaux, items et recettes${withPrices ? '\n+ prix pour le business courant' : ''}.\n(idempotent)`)) return
    try {
      const r = await api(`/api/catalog/seed${withPrices ? `?businessId=${$currentBusinessId}` : ''}`, { method: 'POST' })
      notifySuccess(`Seed : ${r.itemsCreated} items, ${r.recipesSet} recettes, ${r.productsCreated} prix, ${r.familiesCreated + r.materialsCreated} familles/matériaux`)
      if (r.warnings?.length) {
        toast(`${r.warnings.length} avertissement(s) — voir console`, 'info')
        console.warn('Seed warnings:', r.warnings)
      }
      loadTaxa()
      loadItems()
    } catch (e) {
      fail(e)
    }
  }
</script>

{#if !isSystem}
  <p class="text-sm text-destructive">Réservé au rôle SYSTEM.</p>
{:else}
  <SettingsLayout
    title="Configuration — Système"
    subtitle="Catalogue global : familles, matériaux et objets de base (réservé SYSTEM)."
    tabs={TABS}
    active={tab}
    onSelect={(k) => (tab = k)}
  >
    {#if tab === 'familles'}
      <TaxonBoard title="Familles" endpoint="/api/catalog/families" taxa={families} onChanged={loadTaxa} />
    {:else if tab === 'materiaux'}
      <TaxonBoard title="Matériaux" endpoint="/api/catalog/materials" taxa={materials} onChanged={loadTaxa} />
    {:else if tab === 'objets'}
      <BaseItemsBoard {items} {families} {materials} onChanged={loadItems} />
    {:else if tab === 'import'}
      <div class="flex flex-col items-start gap-3">
        <p class="text-sm text-muted-foreground">
          Importe le catalogue Skyrim (familles, matériaux, items, recettes{$currentBusinessId ? ' + prix du business courant' : ''}).
          Idempotent : ne touche pas l'existant.
        </p>
        <Button onclick={runSeed}>Importer le seed Skyrim</Button>
      </div>
    {:else if tab === 'business'}
      <BusinessAdmin />
    {/if}
  </SettingsLayout>
{/if}
