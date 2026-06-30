<script>
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Card from '../ui/Card.svelte'
  import CardHeader from '../ui/CardHeader.svelte'
  import CardTitle from '../ui/CardTitle.svelte'
  import CardContent from '../ui/CardContent.svelte'
  import Button from '../ui/Button.svelte'

  let { businessId } = $props()
  let busy = $state(false)

  async function seedStock() {
    if (!window.confirm('Ajouter 100 unités de chaque produit au stock de ce business ? (outil de test)')) return
    busy = true
    try {
      const res = await api(`/api/businesses/${businessId}/dev/seed-stock`, { method: 'POST' })
      notifySuccess(`${res.count} produits ajoutés à 100 au stock`)
    } catch (e) {
      notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
    } finally {
      busy = false
    }
  }
</script>

<Card>
  <CardHeader><CardTitle>Système — Outils de test</CardTitle></CardHeader>
  <CardContent class="flex flex-col gap-3">
    <p class="text-sm text-muted-foreground">
      Réservé au rôle Système. Remplit le compte stock par défaut de <strong class="text-foreground">100 unités</strong>
      de chaque produit du catalogue (hors septims), pour faciliter les tests.
    </p>
    <div>
      <Button onclick={seedStock} disabled={busy}>{busy ? 'En cours…' : 'Remplir le stock (100 de chaque)'}</Button>
    </div>
  </CardContent>
</Card>
