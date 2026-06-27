<script>
  import { Trash2 } from '@lucide/svelte'
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Card from '../ui/Card.svelte'
  import CardHeader from '../ui/CardHeader.svelte'
  import CardTitle from '../ui/CardTitle.svelte'
  import CardContent from '../ui/CardContent.svelte'
  import Button from '../ui/Button.svelte'

  let { businessId } = $props()
  const MAX_LOGO_BYTES = 500 * 1024
  let dataUrl = $state(null)
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  $effect(() => {
    let active = true
    api(`/api/businesses/${businessId}/logo`).then((l) => active && (dataUrl = l.dataUrl)).catch(fail)
    return () => { active = false }
  })

  async function save(value) {
    try {
      const l = await api(`/api/businesses/${businessId}/logo`, { method: 'PUT', body: JSON.stringify({ dataUrl: value }) })
      dataUrl = l.dataUrl
      notifySuccess('Logo mis à jour')
    } catch (e) {
      fail(e)
    }
  }

  function onFile(e) {
    const file = e.currentTarget.files?.[0]
    if (!file) return
    if (file.size > MAX_LOGO_BYTES) return notifyError('Image trop lourde (max 500 Ko)')
    const reader = new FileReader()
    reader.onload = () => save(reader.result)
    reader.readAsDataURL(file)
  }
</script>

<Card>
  <CardHeader><CardTitle>Logo</CardTitle></CardHeader>
  <CardContent class="flex items-center gap-4">
    <div class="flex size-20 items-center justify-center overflow-hidden rounded-md border bg-muted">
      {#if dataUrl}
        <img src={dataUrl} alt="Logo du business" class="size-full object-contain" />
      {:else}
        <span class="text-xs text-muted-foreground">Aucun</span>
      {/if}
    </div>
    <div class="flex flex-col gap-2">
      <input type="file" accept="image/*" onchange={onFile} class="text-sm" />
      {#if dataUrl}
        <Button variant="ghost" size="sm" onclick={() => save(null)}><Trash2 size={16} /> Retirer</Button>
      {/if}
      <span class="text-xs text-muted-foreground">PNG/JPG, max 500 Ko.</span>
    </div>
  </CardContent>
</Card>
