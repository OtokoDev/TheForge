<script>
  import { api } from '../../lib/api.js'
  import Button from '../ui/Button.svelte'
  import Input from '../ui/Input.svelte'

  let { onSelect, placeholder = 'Rechercher (@pseudo ou nom en jeu)' } = $props()

  let q = $state('')
  let results = $state([])
  let open = $state(false)
  let picked = $state(null)
  let rect = $state(null)
  let wrapEl = $state()
  let timer

  const label = (u) => (u.inGameName ? `${u.inGameName} (@${u.username})` : `@${u.username}`)

  function updateRect() {
    if (!wrapEl) return
    const r = wrapEl.getBoundingClientRect()
    rect = { top: r.bottom + 4, left: r.left, width: r.width }
  }

  function onInput() {
    if (picked) return
    clearTimeout(timer)
    const term = q.trim()
    if (term.length < 1) {
      results = []
      open = false
      return
    }
    timer = setTimeout(async () => {
      try {
        results = await api(`/api/users?q=${encodeURIComponent(term)}`)
        updateRect()
        open = true
      } catch {
        /* ignore */
      }
    }, 250)
  }

  function pick(u) {
    picked = u
    open = false
    results = []
    onSelect(u)
  }

  function reset() {
    picked = null
    q = ''
    onSelect(null)
  }

  $effect(() => {
    if (!open) return
    const reposition = () => updateRect()
    const onDown = (e) => {
      if (wrapEl?.contains(e.target)) return
      open = false
    }
    window.addEventListener('scroll', reposition, true)
    window.addEventListener('resize', reposition)
    document.addEventListener('mousedown', onDown)
    return () => {
      window.removeEventListener('scroll', reposition, true)
      window.removeEventListener('resize', reposition)
      document.removeEventListener('mousedown', onDown)
    }
  })
</script>

{#if picked}
  <div class="flex items-center gap-2">
    <span class="rounded-lg border border-input px-2.5 py-1 text-sm">{label(picked)}</span>
    <Button variant="ghost" size="sm" onclick={reset}>Changer</Button>
  </div>
{:else}
  <div bind:this={wrapEl} class="relative">
    <Input class="w-64" {placeholder} bind:value={q} oninput={onInput} />
    {#if open && results.length > 0 && rect}
      <ul
        style="position:fixed; top:{rect.top}px; left:{rect.left}px; width:{rect.width}px; z-index:60"
        class="max-h-56 overflow-auto rounded-lg border border-border bg-popover text-popover-foreground shadow-md"
      >
        {#each results as u (u.id)}
          <li>
            <button
              type="button"
              class="flex w-full flex-col items-start px-3 py-1.5 text-left text-sm hover:bg-muted"
              onclick={() => pick(u)}
            >
              <span>{u.inGameName ?? u.username}</span>
              <span class="text-xs text-muted-foreground">@{u.username}</span>
            </button>
          </li>
        {/each}
      </ul>
    {/if}
  </div>
{/if}
