<script>
  import { Trash2 } from '@lucide/svelte'
  import { me, currentBusinessId } from '../lib/session.js'
  import { api, ApiError } from '../lib/api.js'
  import { formatMoney, formatDateTime } from '../lib/format.js'
  import { canOperateBusiness } from '../lib/roles.js'
  import { notifySuccess, notifyError } from '../lib/notifications.js'
  import UserAutocomplete from '../components/admin/UserAutocomplete.svelte'
  import Card from '../components/ui/Card.svelte'
  import CardHeader from '../components/ui/CardHeader.svelte'
  import CardTitle from '../components/ui/CardTitle.svelte'
  import CardContent from '../components/ui/CardContent.svelte'
  import Input from '../components/ui/Input.svelte'
  import Button from '../components/ui/Button.svelte'
  import Badge from '../components/ui/Badge.svelte'
  import SelectField from '../components/ui/SelectField.svelte'

  let canOperate = $derived($currentBusinessId ? canOperateBusiness($me, $currentBusinessId) : false)

  let farmers = $state([])
  let items = $state([])
  let defaults = $state({ stockAccountId: null, coffreAccountId: null })
  let openFarmer = $state(null)
  let entries = $state([])

  let depFarmer = $state(null)
  let depKey = $state(0)
  let lines = $state([])
  let depMotif = $state('')
  let payFarmer = $state(null)
  let payKey = $state(1)
  let amount = $state('')
  let payMotif = $state('')

  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  function load() {
    const id = $currentBusinessId
    if (!id) return
    api(`/api/businesses/${id}/creances`).then((v) => (farmers = v)).catch(fail)
    api('/api/catalog/items').then((rows) => (items = rows.filter((i) => !i.system))).catch(fail)
    api(`/api/businesses/${id}/defaults`).then((v) => (defaults = v)).catch(fail)
  }
  $effect(() => {
    $currentBusinessId
    load()
  })

  let totalDue = $derived(farmers.reduce((s, f) => s + f.remaining, 0))
  let itemOptions = $derived([{ value: '', label: 'Objet…' }, ...items.map((it) => ({ value: it.id, label: it.name }))])

  function showEntries(fid) {
    if (openFarmer === fid) {
      openFarmer = null
      return
    }
    openFarmer = fid
    api(`/api/businesses/${$currentBusinessId}/creances/${fid}/entries`).then((v) => (entries = v)).catch(fail)
  }

  function addLine() {
    lines = [...lines, { itemId: items[0]?.id ?? '', quantity: 1 }]
  }
  function removeLine(i) {
    lines = lines.filter((_, j) => j !== i)
  }
  function setLineItem(i, v) {
    lines = lines.map((l, j) => (j === i ? { ...l, itemId: v } : l))
  }
  function setLineQty(i, v) {
    lines = lines.map((l, j) => (j === i ? { ...l, quantity: Number(v) } : l))
  }

  async function deposit() {
    if (!depFarmer) return notifyError('Choisis un farmeur')
    if (lines.length === 0 || lines.some((l) => !l.itemId || l.quantity <= 0)) return notifyError('Ajoute au moins une ligne valide')
    if (!defaults.stockAccountId) return notifyError('Aucun compte stock par défaut (Configuration)')
    try {
      await api(`/api/businesses/${$currentBusinessId}/creances/deposit`, {
        method: 'POST',
        body: JSON.stringify({ farmerUserId: depFarmer.id, lines, stockAccountId: defaults.stockAccountId, reference: depMotif || null }),
      })
      notifySuccess('Dépôt enregistré')
      depFarmer = null
      depKey += 2
      lines = []
      depMotif = ''
      load()
    } catch (e) {
      fail(e)
    }
  }

  async function payment() {
    if (!payFarmer) return notifyError('Choisis un farmeur')
    const n = Number(amount)
    if (!Number.isFinite(n) || n <= 0) return notifyError('Montant invalide')
    if (!defaults.coffreAccountId) return notifyError('Aucun coffre par défaut (Configuration)')
    try {
      await api(`/api/businesses/${$currentBusinessId}/creances/payment`, {
        method: 'POST',
        body: JSON.stringify({ farmerUserId: payFarmer.id, amount: n, coffreAccountId: defaults.coffreAccountId, reference: payMotif || null }),
      })
      notifySuccess('Paiement enregistré')
      payFarmer = null
      payKey += 2
      amount = ''
      payMotif = ''
      load()
    } catch (e) {
      fail(e)
    }
  }
</script>

{#if !$currentBusinessId}
  <p class="text-sm text-muted-foreground">Sélectionne un business (en haut) pour gérer les créances.</p>
{:else}
  <div class="flex flex-col gap-6">
    <div>
      <h1 class="text-2xl font-bold tracking-tight">Créances</h1>
      <p class="mt-1 text-sm text-muted-foreground">
        Total dû aux farmeurs : <strong class="text-foreground">{formatMoney(totalDue)}</strong>
      </p>
    </div>

    {#if canOperate}
      <div class="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Déposer (rachat de matières)</CardTitle></CardHeader>
          <CardContent class="flex flex-col gap-3">
            {#key depKey}
              <UserAutocomplete onSelect={(u) => (depFarmer = u)} />
            {/key}
            {#each lines as line, i (i)}
              <div class="flex flex-wrap items-center gap-2">
                <SelectField value={line.itemId} onChange={(v) => setLineItem(i, v)} options={itemOptions} />
                <input
                  type="number"
                  min="1"
                  class="h-8 w-24 rounded-lg border border-input bg-input/30 px-2.5 text-sm outline-none"
                  value={line.quantity}
                  oninput={(e) => setLineQty(i, e.currentTarget.value)}
                />
                <Button variant="ghost" size="icon" onclick={() => removeLine(i)}><Trash2 size={16} /></Button>
              </div>
            {/each}
            <div>
              <Button variant="outline" size="sm" disabled={items.length === 0} onclick={addLine}>Ajouter une ligne</Button>
            </div>
            <Input placeholder="Motif (optionnel)" bind:value={depMotif} />
            <Button class="self-start" onclick={deposit}>Déposer</Button>
            <p class="text-xs text-muted-foreground">Valorisé au coût ; marchandise entrée en stock ; crédite le farmeur.</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Payer un farmeur</CardTitle></CardHeader>
          <CardContent class="flex flex-col gap-3">
            {#key payKey}
              <UserAutocomplete onSelect={(u) => (payFarmer = u)} />
            {/key}
            <Input type="number" placeholder="Montant (septims)" bind:value={amount} />
            <Input placeholder="Motif (optionnel)" bind:value={payMotif} />
            <Button class="self-start" onclick={payment}>Payer</Button>
            <p class="text-xs text-muted-foreground">Septims sortis du coffre par défaut.</p>
          </CardContent>
        </Card>
      </div>
    {/if}

    <Card>
      <CardHeader><CardTitle>Farmeurs</CardTitle></CardHeader>
      <CardContent class="flex flex-col gap-2">
        {#if farmers.length === 0}
          <p class="text-sm text-muted-foreground">Aucune créance.</p>
        {/if}
        {#each farmers as f (f.farmerUserId)}
          <div class="rounded-md border">
            <button
              onclick={() => showEntries(f.farmerUserId)}
              class="flex w-full flex-wrap items-center justify-between gap-2 px-3 py-2 text-left hover:bg-muted/50"
            >
              <span class="font-medium">{f.farmerUsername}</span>
              <span class="flex items-center gap-3 text-sm">
                <span class="text-muted-foreground">crédité {formatMoney(f.totalCredit)}</span>
                <span class="text-muted-foreground">payé {formatMoney(f.totalPaid)}</span>
                <Badge variant={f.remaining > 0 ? 'destructive' : 'secondary'}>reste {formatMoney(f.remaining)}</Badge>
              </span>
            </button>
            {#if openFarmer === f.farmerUserId}
              <div class="border-t px-3 py-2">
                {#if entries.length === 0}
                  <p class="text-xs text-muted-foreground">Aucune entrée.</p>
                {/if}
                {#each entries as e, i (i)}
                  <div class="flex flex-wrap items-center justify-between gap-2 py-1 text-sm">
                    <span class="flex items-center gap-2">
                      <Badge variant={e.type === 'CREDIT' ? 'outline' : 'secondary'}>{e.type === 'CREDIT' ? 'Dépôt' : 'Paiement'}</Badge>
                      <span class="text-muted-foreground">{e.reference ?? '—'}</span>
                    </span>
                    <span class="flex items-center gap-3 text-muted-foreground">
                      <span>{formatMoney(e.amount)}</span>
                      <span>{e.username}</span>
                      <span>{formatDateTime(e.createdAt)}</span>
                    </span>
                  </div>
                {/each}
              </div>
            {/if}
          </div>
        {/each}
      </CardContent>
    </Card>
  </div>
{/if}
