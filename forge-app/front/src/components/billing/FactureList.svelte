<script>
  import { api, ApiError } from '../../lib/api.js'
  import { notifyError, notifySuccess } from '../../lib/notifications.js'

  let { businessId, factures, shiftOpen, shiftSince, canOperate, canAdmin, meId, onNew, onOpenShift, onCloseShift, onEdit, onChange } = $props()

  const ORANGE = '#E8590C', GREEN = '#5BBF73', RED = '#ed8472', TEXT = '#F4F1EE', MUTED = '#8f8880'
  const CARD = '#1c1a18', TABLE_BG = '#1a1816', HEAD_BG = '#221f1b', BORDER = '1px solid rgba(255,255,255,0.07)'
  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')
  const isToday = (iso) => new Date(iso).toDateString() === new Date().toDateString()
  const fail = (e) => notifyError(e instanceof ApiError ? e.message : 'Erreur inattendue')

  const th = `color:${MUTED}; font-weight:600; font-size:12px; letter-spacing:.03em; padding:13px 16px; border-bottom:${BORDER}; white-space:nowrap;`
  const btnPrimary = `background:${ORANGE}; border:none; color:#fff; font-size:12.5px; font-weight:700; padding:7px 12px; border-radius:8px; cursor:pointer;`
  const btnGhost = 'background:transparent; border:1px solid rgba(255,255,255,0.13); color:#cfc8c2; font-size:12.5px; font-weight:600; padding:7px 12px; border-radius:8px; cursor:pointer;'
  const btnDanger = 'background:rgba(229,96,77,0.13); border:1px solid rgba(229,96,77,0.35); color:#ed8472; font-size:12.5px; font-weight:600; padding:7px 12px; border-radius:8px; cursor:pointer;'

  let query = $state('')
  let status = $state('today')
  let sort = $state({ key: 'num', dir: 'desc' })
  let open = $state(null)

  const statusChips = [{ id: 'today', l: "Aujourd'hui" }, { id: 'all', l: 'Toutes' }, { id: 'paid', l: 'Payées' }, { id: 'unpaid', l: 'Non payées' }]

  let kpis = $derived.by(() => {
    const validated = factures.filter((f) => f.status === 'VALIDEE')
    const todayV = validated.filter((f) => isToday(f.createdAt))
    const caJour = todayV.filter((f) => f.paid).reduce((s, f) => s + f.totalAmount, 0)
    const unpaid = validated.filter((f) => !f.paid)
    const panier = todayV.length ? Math.round(todayV.reduce((s, f) => s + f.totalAmount, 0) / todayV.length) : 0
    // Reste dû = total − acompte déjà encaissé (via la commande).
    const nonPaye = unpaid.reduce((s, f) => s + Math.max(0, f.totalAmount - (f.deposit ?? 0)), 0)
    return { caJour, emisJour: todayV.length, nonPaye, nonPayeCount: unpaid.length, panier }
  })

  let rows = $derived.by(() => {
    const q = query.trim().toLowerCase()
    let r = factures.filter((f) => {
      if (status === 'today') return isToday(f.createdAt)
      if (status === 'paid') return f.status === 'VALIDEE' && f.paid
      if (status === 'unpaid') return f.status === 'VALIDEE' && !f.paid
      return true
    })
    if (q) r = r.filter((f) => `#${f.numero} ${f.clientName ?? ''} ${f.lines.map((l) => l.itemName).join(' ')}`.toLowerCase().includes(q))
    const dir = sort.dir === 'asc' ? 1 : -1
    return [...r].sort((a, b) => {
      switch (sort.key) {
        case 'client': return (a.clientName ?? '').localeCompare(b.clientName ?? '') * dir
        case 'total': return (a.totalAmount - b.totalAmount) * dir
        case 'date': return a.createdAt.localeCompare(b.createdAt) * dir
        default: return (a.numero - b.numero) * dir
      }
    })
  })

  function toggleSort(key) {
    sort = sort.key === key ? { key, dir: sort.dir === 'asc' ? 'desc' : 'asc' } : { key, dir: 'desc' }
  }
  function toggleOpen(id) {
    open = open === id ? null : id
  }
  async function validate(id, paid) {
    try { await api(`/api/businesses/${businessId}/factures/${id}/validate`, { method: 'POST', body: JSON.stringify({ paid }) }); notifySuccess('Facture émise'); onChange() } catch (e) { fail(e) }
  }
  async function encaisser(id) {
    try { await api(`/api/businesses/${businessId}/factures/${id}/pay`, { method: 'POST' }); notifySuccess('Encaissée'); onChange() } catch (e) { fail(e) }
  }
  async function del(id) {
    if (!confirm('Supprimer ce brouillon ? Action irréversible.')) return
    try { await api(`/api/businesses/${businessId}/factures/${id}`, { method: 'DELETE' }); notifySuccess('Brouillon supprimé'); onChange() } catch (e) { fail(e) }
  }
  // Édition/suppression d'un brouillon : son créateur ou un admin du business.
  const canEditDraft = (f) => canAdmin || f.createdBy === meId

  const dt = (iso) => new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
  const since = (iso) => new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
</script>

<div>
  <div style="display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:15px;">
    <div>
      <div style="color:{TEXT}; font-size:24px; font-weight:700;">Facturation</div>
      <div style="color:{MUTED}; font-size:13.5px; margin-top:3px;">Émission directe · {kpis.emisJour} factures aujourd'hui</div>
    </div>
    <div style="display:flex; align-items:center; gap:12px;">
      {#if canOperate}
        {#if shiftOpen}
          <div style="display:flex; align-items:center; gap:10px; background:{CARD}; border:1px solid rgba(91,191,115,0.3); border-radius:999px; padding:7px 8px 7px 14px;">
            <span style="display:flex; align-items:center; gap:7px; color:#7fd398; font-size:13px; font-weight:600;">
              <span style="width:8px; height:8px; border-radius:999px; background:{GREEN}; box-shadow:0 0 8px {GREEN};"></span>
              Service ouvert
            </span>
            {#if shiftSince}<span style="color:#6f6862; font-size:12px;">· {since(shiftSince)}</span>{/if}
            <button onclick={onCloseShift} style="background:rgba(255,255,255,0.06); border:none; color:#cfc8c2; font-size:12px; font-weight:600; padding:5px 10px; border-radius:999px; cursor:pointer;">Fermer</button>
          </div>
        {:else}
          <button onclick={onOpenShift} style="background:transparent; border:1px solid rgba(255,255,255,0.13); color:#cfc8c2; font-size:13px; font-weight:600; padding:8px 14px; border-radius:999px; cursor:pointer;">Ouvrir le service</button>
        {/if}
        <button onclick={onNew} style="display:flex; align-items:center; gap:8px; background:{ORANGE}; border:none; color:#fff; font-size:13.5px; font-weight:700; padding:10px 17px; border-radius:9px; cursor:pointer; box-shadow:0 4px 14px rgba(232,89,12,0.32);">+ Nouvelle facture</button>
      {/if}
    </div>
  </div>

  <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:12px; margin-bottom:15px;">
    {@render kpi("Encaissé aujourd'hui", `${fmt(kpis.caJour)} septims`)}
    {@render kpi('Factures émises (jour)', fmt(kpis.emisJour))}
    {@render kpi('Non payé (à crédit)', fmt(kpis.nonPaye), `· ${kpis.nonPayeCount} fact.`, RED)}
    {@render kpi('Panier moyen', fmt(kpis.panier))}
  </div>

  <div style="display:flex; align-items:center; gap:11px; flex-wrap:wrap; margin-bottom:15px;">
    <input bind:value={query} placeholder="N° de facture, client, article…" style="background:{CARD}; border:1px solid rgba(255,255,255,0.1); border-radius:9px; color:{TEXT}; font-size:13.5px; padding:9px 12px; width:280px; outline:none;" />
    {#each statusChips as c (c.id)}
      <button onclick={() => (status = c.id)} style="background:{status === c.id ? ORANGE : '#1f1d1b'}; color:{status === c.id ? '#fff' : '#cfc8c2'}; border:{status === c.id ? `1px solid ${ORANGE}` : '1px solid rgba(255,255,255,0.08)'}; border-radius:999px; padding:7px 14px; font-size:13px; font-weight:600; cursor:pointer;">{c.l}</button>
    {/each}
  </div>

  <div style="overflow:auto; border:{BORDER}; border-radius:12px; background:{TABLE_BG};">
    <table style="width:100%; border-collapse:collapse; font-size:13.5px;">
      <thead>
        <tr style="background:{HEAD_BG};">
          <th onclick={() => toggleSort('num')} style="{th} text-align:left; cursor:pointer;">N°</th>
          <th onclick={() => toggleSort('date')} style="{th} text-align:left; cursor:pointer;">DATE</th>
          <th onclick={() => toggleSort('client')} style="{th} text-align:left; cursor:pointer;">CLIENT</th>
          <th style="{th} text-align:left;">ARTICLES</th>
          <th style="{th} text-align:center;">STATUT</th>
          <th onclick={() => toggleSort('total')} style="{th} text-align:right; cursor:pointer;">TOTAL</th>
        </tr>
      </thead>
      <tbody>
        {#each rows as f (f.id)}
          {@const statut = f.status === 'BROUILLON' ? 'Brouillon' : f.paid ? 'Payé' : 'Non payé'}
          {@const sColor = f.status === 'BROUILLON' ? MUTED : f.paid ? '#7fd398' : RED}
          {@const sBg = f.status === 'BROUILLON' ? 'rgba(255,255,255,0.06)' : f.paid ? 'rgba(91,191,115,0.13)' : 'rgba(229,96,77,0.13)'}
          <tr onclick={() => toggleOpen(f.id)} style="border-bottom:{BORDER}; cursor:pointer; background:{open === f.id ? 'rgba(255,255,255,0.03)' : 'transparent'};">
            <td style="padding:11px 16px; color:{TEXT}; font-weight:600; font-variant-numeric:tabular-nums;">#{String(f.numero).padStart(4, '0')}</td>
            <td style="padding:11px 16px; color:#9a938c;">{dt(f.createdAt)}</td>
            <td style="padding:11px 16px; color:#e7e1db;">{f.clientName ?? 'Client de passage'}</td>
            <td style="padding:11px 16px; color:#9a938c; max-width:280px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">{f.lines.map((l) => `${l.quantity}× ${l.itemName}`).join(', ')}</td>
            <td style="padding:11px 16px; text-align:center;"><span style="font-size:12px; font-weight:700; color:{sColor}; background:{sBg}; padding:4px 10px; border-radius:999px;">{statut}</span></td>
            <td style="padding:11px 16px; text-align:right; color:{TEXT}; font-weight:700; font-variant-numeric:tabular-nums;">{fmt(f.totalAmount)}</td>
          </tr>
          {#if open === f.id}
            {@const dispTotal = f.totalAmount || f.lines.reduce((s, l) => s + (l.lineTotal || Math.round(Number(l.unitPrice) * l.quantity)), 0)}
            <tr>
              <td colspan="6" style="background:#181513; padding:12px 16px; border-bottom:{BORDER};">
                <div style="display:flex; flex-wrap:wrap; gap:18px; align-items:center; justify-content:space-between;">
                  <div style="color:#cfc8c2; font-size:13px;">
                    {f.lines.map((l) => `${l.quantity}× ${l.itemName} (${fmt(l.lineTotal || l.unitPrice * l.quantity)})`).join(' · ')}
                    {#if f.status === 'VALIDEE'}<span style="color:{MUTED};"> — bénéf. {fmt(f.totalProfit)} · part forge {fmt(f.businessShare)} · part forgeron {fmt(Math.round(Number(f.totalProfit)) - Math.round(Number(f.businessShare)))} septims</span>{/if}
                    {#if f.deposit > 0}<div style="color:#f5a06a; font-size:12.5px; margin-top:4px;">Acompte déjà versé : {fmt(f.deposit)} · Net à encaisser : {fmt(Math.max(0, dispTotal - f.deposit))} septims</div>{/if}
                  </div>
                  {#if canOperate}
                    <div style="display:flex; gap:8px;" role="presentation" onclick={(e) => e.stopPropagation()}>
                      {#if f.status === 'BROUILLON'}
                        {#if canEditDraft(f)}
                          <button onclick={() => onEdit(f)} style={btnGhost}>Modifier</button>
                          <button onclick={() => del(f.id)} style={btnDanger}>Supprimer</button>
                        {/if}
                        <button onclick={() => validate(f.id, true)} style={btnPrimary}>Émettre &amp; encaisser</button>
                        <button onclick={() => validate(f.id, false)} style={btnGhost}>Émettre à crédit</button>
                      {:else if !f.paid}
                        <button onclick={() => encaisser(f.id)} style={btnPrimary}>Encaisser</button>
                      {/if}
                    </div>
                  {/if}
                </div>
              </td>
            </tr>
          {/if}
        {/each}
      </tbody>
    </table>
    <div style="padding:11px 16px; color:#6f6862; font-size:12.5px; border-top:1px solid rgba(255,255,255,0.05);">{rows.length} factures</div>
  </div>
</div>

{#snippet kpi(label, value, sub, color)}
  <div style="background:{CARD}; border:{BORDER}; border-radius:12px; padding:15px 17px;">
    <div style="color:{MUTED}; font-size:11.5px; text-transform:uppercase; letter-spacing:.06em; font-weight:600;">{label}</div>
    <div style="color:{color ?? TEXT}; font-size:25px; font-weight:700; margin-top:6px;">{value} {#if sub}<span style="font-size:13px; color:{MUTED}; font-weight:500;">{sub}</span>{/if}</div>
  </div>
{/snippet}
