<script>
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'
  import Card from '../ui/Card.svelte'
  import CardHeader from '../ui/CardHeader.svelte'
  import CardTitle from '../ui/CardTitle.svelte'
  import CardContent from '../ui/CardContent.svelte'
  import Input from '../ui/Input.svelte'
  import Button from '../ui/Button.svelte'
  import { Trash2, Upload, Pencil, X } from '@lucide/svelte'

  let { businessId } = $props()
  const MAX = 1024 * 1024
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  let types = $state([])
  let editId = $state(null)
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

  function reset() {
    editId = null
    label = ''
    color = '#E8590C'
    imageDataUrl = null
  }
  function startEdit(t) {
    editId = t.id
    label = t.label
    color = t.color
    imageDataUrl = t.imageDataUrl ?? null
  }

  function onFile(e) {
    const f = e.currentTarget.files?.[0]
    if (!f) return
    if (f.size > MAX) return notifyError('Image trop lourde (max 1 Mo)')
    const r = new FileReader()
    r.onload = () => (imageDataUrl = r.result)
    r.readAsDataURL(f)
    e.currentTarget.value = ''
  }

  async function save() {
    if (!label.trim()) return notifyError('Libellé requis')
    const body = JSON.stringify({ label: label.trim(), color, imageDataUrl })
    try {
      if (editId) {
        await api(`/api/businesses/${businessId}/marker-types/${editId}`, { method: 'PUT', body })
        notifySuccess('Type modifié')
      } else {
        await api(`/api/businesses/${businessId}/marker-types`, { method: 'POST', body })
        notifySuccess('Type ajouté')
      }
      reset()
      load()
    } catch (e) {
      fail(e)
    }
  }

  async function del(t) {
    const warn =
      t.usageCount > 0
        ? `Supprimer le type « ${t.label} » ?\n\n⚠ Attention : suppression de ${t.usageCount} marqueur(s), continuer ?`
        : `Supprimer le type « ${t.label} » ?`
    if (!confirm(warn)) return
    try {
      await api(`/api/businesses/${businessId}/marker-types/${t.id}`, { method: 'DELETE' })
      if (editId === t.id) reset()
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
      {#if imageDataUrl}
        <div class="flex items-center gap-1">
          <img src={imageDataUrl} alt="" class="size-9 rounded border object-contain" />
          <button onclick={() => (imageDataUrl = null)} aria-label="Retirer l'image" class="text-muted-foreground hover:text-destructive"><X size={14} /></button>
        </div>
      {/if}
      <Button onclick={save}>{editId ? 'Enregistrer' : 'Ajouter'}</Button>
      {#if editId}
        <Button variant="ghost" onclick={reset}>Annuler</Button>
      {/if}
    </div>

    <div class="flex flex-col gap-2">
      {#each types as t (t.id)}
        <div class="flex items-center gap-3 rounded-lg border p-2 {editId === t.id ? 'ring-1 ring-primary' : ''}">
          {#if t.imageDataUrl}
            <img src={t.imageDataUrl} alt="" class="size-7 rounded object-contain" />
          {:else}
            <span class="size-4 rounded-full" style="background:{t.color};"></span>
          {/if}
          <span class="flex-1 text-sm font-medium">{t.label}</span>
          {#if t.usageCount > 0}
            <span class="text-xs text-muted-foreground">{t.usageCount} marqueur(s)</span>
          {/if}
          <button onclick={() => startEdit(t)} aria-label="Modifier" class="text-muted-foreground transition hover:text-foreground"><Pencil size={15} /></button>
          <button onclick={() => del(t)} aria-label="Supprimer" class="text-muted-foreground transition hover:text-destructive"><Trash2 size={15} /></button>
        </div>
      {/each}
      {#if types.length === 0}
        <p class="text-sm text-muted-foreground">Aucun type. Ajoute-en pour pouvoir poser des marqueurs sur la carte.</p>
      {/if}
    </div>
  </CardContent>
</Card>
