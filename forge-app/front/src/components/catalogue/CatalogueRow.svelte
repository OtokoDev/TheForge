<script>
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, toast } from '../../lib/notifications.js'
  import NumberInput from '../ui/NumberInput.svelte'

  let { businessId, item, product, cost, canEdit, onSaved } = $props()

  const TEXT = '#F4F1EE'
  const MUTED = '#8f8880'
  const BORDER = '1px solid rgba(255,255,255,0.07)'
  const DEFAULT_CAT = '#7d90a6'
  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const td = `padding:9px 16px; border-bottom:${BORDER}; color:#cfc8c2;`
  const tdR = td + 'text-align:right;'
  const inputStyle =
    'width:90px; text-align:right; background:#15110e; border:1px solid rgba(255,255,255,0.12); border-radius:6px; color:' +
    TEXT + '; font-size:13px; padding:5px 8px; outline:none;'

  let valeur = $state('')
  let prix = $state('')
  $effect(() => {
    valeur = product?.valeur != null ? String(product.valeur) : ''
    prix = product?.prixRevente != null ? String(product.prixRevente) : ''
  })

  async function save() {
    if (!canEdit) return
    try {
      await api(`/api/businesses/${businessId}/products/${item.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          valeur: item.hasRecipe || valeur === '' ? null : Number(valeur),
          prixRevente: prix === '' ? null : Number(prix),
          version: product?.version ?? 0,
        }),
      })
      onSaved()
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        toast(e.message, 'info')
        onSaved()
      } else {
        notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')
      }
    }
  }
</script>

<tr>
  <td style={td}>
    <span style="display:inline-flex; align-items:center; gap:9px;">
      <span style="width:10px; height:10px; border-radius:3px; flex:none; background:{item.familyColor ?? DEFAULT_CAT};"></span>
      <span style="color:{TEXT}; font-weight:600;">{item.name}</span>
    </span>
  </td>
  <td style={td}>{item.familyName ?? '—'}</td>
  <td style={td}>{item.materialName ?? '—'}</td>
  <td style={tdR}>
    {#if item.hasRecipe}
      <span style="color:{MUTED};">{cost != null ? `${fmt(cost)} (calc.)` : '—'}</span>
    {:else if canEdit}
      <NumberInput variant="dark" class="w-28" value={valeur} onchange={(v) => (valeur = v)} onblur={save} min={0} />
    {:else}
      <span>{product?.valeur != null ? fmt(product.valeur) : '—'}</span>
    {/if}
  </td>
  <td style={tdR}>
    {#if canEdit}
      <NumberInput variant="dark" class="w-28" value={prix} onchange={(v) => (prix = v)} onblur={save} min={0} />
    {:else}
      <span style="color:{product?.prixRevente != null ? TEXT : MUTED};">{product?.prixRevente != null ? fmt(product.prixRevente) : 'non vendable'}</span>
    {/if}
  </td>
</tr>
