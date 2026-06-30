<script>
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Card from '../ui/Card.svelte'
  import CardHeader from '../ui/CardHeader.svelte'
  import CardTitle from '../ui/CardTitle.svelte'
  import CardContent from '../ui/CardContent.svelte'
  import Input from '../ui/Input.svelte'
  import Button from '../ui/Button.svelte'
  import { Trash2, Upload } from '@lucide/svelte'

  let { businessId } = $props()
  const MAX = 1024 * 1024
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  let types = $state([])
  let label = $state('')
  let color = $state('#E8590C')
  let imageDataUrl = $state(null)
  let fileInput = $state(null)

  function load() {
    api(`/api/businesses/${businessId}/marker-types`).then((v) => (types = v)).catch(fail)
  }
  $effect(() => {
    businessId
    load()
  })

  function onFile(e) {
    const f = e.currentTarget.files?.[0]
    if (!f) return
    if (f.size > MAX) return notifyError('Image trop lourde (max 1 Mo)')
    const r = new FileReader()
    r.onload = () => (imageDataUrl = r.result)
    r.readAsDataURL(f)
    e.currentTarget.value = ''
  }

  async function create() {
    if (!label.trim()) return notifyError('Libellé requis')
    try {
      await api(`/api/businesses/${businessId}/marker-types`, {
        method: 'POST',
        body: JSON.stringify({ label: label.trim(), color, imageDataUrl }),
      })
      notifySuccess('Type ajouté')
      label = ''
      imageDataUrl = null
      load()
    } catch (e) {
      fail(e)
    }
  }
  async function del(id) {
    if (!confirm('Supprimer ce type ? Les marqueurs existants de ce type retomberont sur une pastille neutre.')) return
    try {
      await api(`/api/businesses/${businessId}/marker-types/${id}`, { method: 'DELETE' })
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

<Card>
  <CardHeader><CardTitle>Types de marqueurs (carte)</CardTitle></CardHeader>
  <CardContent class="flex flex-col gap-4">
    <p class="text-sm text-muted-foreground">
      Définis les types de points de la carte (zone de chasse, filon, mine…) : nom, couleur et
      image optionnelle. <strong class="text-foreground">Sans image</strong>, une pastille ronde de la couleur s'affiche.
    </p>

    <div class="flex flex-wrap items-end gap-3 border-b pb-4">
      <label class="flex flex-col gap-1 text-xs text-muted-foreground">Nom
        <Input bind:value={label} placeholder="ex. Filon de fer" ariaLabel="Nom du type" class="w-48" />
      </label>
      <label class="flex flex-col gap-1 text-xs text-muted-foreground">Couleur
        <input type="color" bind:value={color} aria-label="Couleur" class="h-9 w-14 cursor-pointer rounded-md border bg-input/30" />
      </label>
      <input type="file" accept="image/*" bind:this={fileInput} onchange={onFile} class="hidden" />
      <Button variant="outline" onclick={() => fileInput?.click()}><Upload size={16} /> {imageDataUrl ? 'Image ✓' : 'Image (option.)'}</Button>
      {#if imageDataUrl}<img src={imageDataUrl} alt="" class="size-9 rounded border object-contain" />{/if}
      <Button onclick={create}>Ajouter</Button>
    </div>

    <div class="flex flex-col gap-2">
      {#each types as t (t.id)}
        <div class="flex items-center gap-3 rounded-lg border p-2">
          {#if t.imageDataUrl}
            <img src={t.imageDataUrl} alt="" class="size-7 rounded object-contain" />
          {:else}
            <span class="size-4 rounded-full" style="background:{t.color};"></span>
          {/if}
          <span class="flex-1 text-sm font-medium">{t.label}</span>
          <button onclick={() => del(t.id)} aria-label="Supprimer" class="text-muted-foreground transition hover:text-destructive"><Trash2 size={15} /></button>
        </div>
      {/each}
      {#if types.length === 0}
        <p class="text-sm text-muted-foreground">Aucun type. Ajoute-en pour pouvoir poser des marqueurs sur la carte.</p>
      {/if}
    </div>
  </CardContent>
</Card>
