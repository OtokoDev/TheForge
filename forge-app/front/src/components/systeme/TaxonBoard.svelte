<script>
  import { GripVertical, Trash2 } from '@lucide/svelte'
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, toast } from '../../lib/notifications.js'
  import Button from '../ui/Button.svelte'
  import Input from '../ui/Input.svelte'

  // Liste réordonnable (drag & drop) d'une énumération : familles ou matériaux (SYSTEM).
  let { title, endpoint, taxa, onChanged } = $props()

  let rows = $state([])
  let nom = $state('')
  let couleur = $state('#888888')
  let dragIndex = null

  $effect(() => {
    rows = taxa
  })

  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function onDrop(target) {
    const from = dragIndex
    dragIndex = null
    if (from === null || from === target) return
    const next = [...rows]
    const [moved] = next.splice(from, 1)
    next.splice(target, 0, moved)
    rows = next
    api(`${endpoint}/reorder`, { method: 'PUT', body: JSON.stringify({ ids: next.map((t) => t.id) }) }).then(onChanged).catch(fail)
  }

  async function save(t, patch) {
    try {
      await api(`${endpoint}/${t.id}`, {
        method: 'PUT',
        body: JSON.stringify({ nom: patch.nom ?? t.nom, ordre: t.ordre, couleur: patch.couleur ?? t.couleur, version: t.version }),
      })
      onChanged()
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        toast(e.message, 'info')
        onChanged()
      } else fail(e)
    }
  }

  async function add() {
    if (!nom.trim()) return
    try {
      await api(endpoint, { method: 'POST', body: JSON.stringify({ nom: nom.trim(), couleur }) })
      nom = ''
      onChanged()
    } catch (e) {
      fail(e)
    }
  }

  async function remove(t) {
    try {
      await api(`${endpoint}/${t.id}`, { method: 'DELETE' })
      onChanged()
    } catch (e) {
      fail(e)
    }
  }
</script>

<div>
  <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
    {title} — glisser-déposer pour réordonner
  </p>
  <div class="flex flex-col gap-1.5">
    {#each rows as t, i (t.id)}
      <div
        draggable="true"
        ondragstart={() => (dragIndex = i)}
        ondragover={(e) => e.preventDefault()}
        ondrop={() => onDrop(i)}
        class="flex items-center gap-3 rounded-md border bg-card px-3 py-2"
        role="listitem"
      >
        <GripVertical size={16} class="cursor-grab text-muted-foreground" />
        <span class="w-6 text-xs tabular-nums text-muted-foreground">{i}</span>
        <input
          type="color"
          value={t.couleur ?? '#888888'}
          onchange={(e) => save(t, { couleur: e.currentTarget.value })}
          class="size-6 shrink-0 cursor-pointer rounded border bg-transparent"
          title="Couleur"
        />
        <input
          class="h-8 max-w-xs rounded-lg border border-input bg-input/30 px-2.5 text-sm outline-none"
          value={t.nom}
          onblur={(e) => e.currentTarget.value.trim() && e.currentTarget.value !== t.nom && save(t, { nom: e.currentTarget.value.trim() })}
        />
        <div class="flex-1"></div>
        <Button variant="ghost" size="icon" onclick={() => remove(t)}><Trash2 size={16} /></Button>
      </div>
    {/each}
    {#if rows.length === 0}<p class="text-sm text-muted-foreground">Aucune entrée.</p>{/if}
  </div>
  <div class="mt-3 flex items-center gap-2 border-t pt-3">
    <input type="color" bind:value={couleur} class="size-6 shrink-0 cursor-pointer rounded border bg-transparent" />
    <Input class="max-w-xs" placeholder="Nouvelle entrée…" bind:value={nom} />
    <Button size="sm" onclick={add}>Ajouter</Button>
  </div>
</div>
