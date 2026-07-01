<script>
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Card from '../ui/Card.svelte'
  import CardHeader from '../ui/CardHeader.svelte'
  import CardTitle from '../ui/CardTitle.svelte'
  import CardContent from '../ui/CardContent.svelte'
  import Input from '../ui/Input.svelte'
  import Button from '../ui/Button.svelte'

  let { businessId } = $props()
  let url = $state('')
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  $effect(() => {
    if (!businessId) return
    api(`/api/businesses/${businessId}/webhook`).then((v) => (url = v.url ?? '')).catch(fail)
  })

  async function save() {
    try {
      await api(`/api/businesses/${businessId}/webhook`, { method: 'PUT', body: JSON.stringify({ url: url.trim() || null }) })
      notifySuccess(url.trim() ? 'Webhook enregistré' : 'Webhook effacé (fallback global)')
    } catch (e) {
      fail(e)
    }
  }
</script>

<Card>
  <CardHeader><CardTitle>Webhook Discord</CardTitle></CardHeader>
  <CardContent class="flex flex-col gap-3">
    <p class="text-sm text-muted-foreground">
      Les récaps (prise de service, facture validée, fin de service, rappel de taxe) sont postés
      sur ce webhook — le canal Discord de ta faction. Vide → webhooks globaux du serveur.
    </p>
    <div class="flex flex-wrap items-end gap-2">
      <label class="flex min-w-64 flex-1 flex-col gap-1 text-xs text-muted-foreground">URL du webhook
        <Input bind:value={url} placeholder="https://discord.com/api/webhooks/…" ariaLabel="URL du webhook" />
      </label>
      <Button onclick={save}>Enregistrer</Button>
    </div>
  </CardContent>
</Card>
