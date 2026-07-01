<script>
  import { FileText, Box, ChartLine, Flame } from '@lucide/svelte'
  import { me, currentBusinessId, currentBusiness, currentLogo, shift } from '../lib/session.js'
  import { api } from '../lib/api.js'
  import { GLOBAL_ROLE_LABELS, canAdminBusiness } from '../lib/roles.js'

  const ORANGE = '#E8590C', SOFT = '#f5a06a', TEXT = '#F4F1EE', MUTED = '#8f8880'
  const CARD = '#1c1a18', BORDER = '1px solid rgba(255,255,255,0.07)', GREEN = '#5BBF73'
  const fmt = (n) => Math.round(Number(n ?? 0)).toLocaleString('fr-FR')

  let displayName = $derived($me.user.inGameName ?? $me.user.username)
  let isCompagnie = $derived($currentBusiness?.type === 'COMPAGNIE')

  let ov = $state(null)
  let stockVal = $state(null)
  let creancesDu = $state(null)
  let activity = $state([])
  let cityTaxDue = $state(0)
  let canAdmin = $derived($currentBusinessId ? canAdminBusiness($me, $currentBusinessId) : false)

  const ACT = {
    LOGIN_OK: 'Connexion', MEMBER_ADD: 'Ajout membre', ROLE_SET: 'Rôle',
    CREANCE_DEPOT: 'Rachat', CREANCE_PAIEMENT: 'Paiement',
    USER_BAN: 'Bannissement', USER_UNBAN: 'Réactivation', BUSINESS_CREATE: 'Business',
  }
  const actLabel = (a) => ACT[a] ?? a
  const time = (iso) => new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })

  $effect(() => {
    const id = $currentBusinessId
    const compagnie = isCompagnie
    if (!id) {
      ov = null
      stockVal = null
      creancesDu = null
      activity = []
      return
    }
    const from = new Date(Date.now() - 30 * 86400000).toISOString()
    const to = new Date().toISOString()
    let active = true
    api(`/api/businesses/${id}/stats/overview?from=${from}&to=${to}`).then((v) => active && (ov = v)).catch(() => {})
    api(`/api/businesses/${id}/stats/stock?from=${from}&to=${to}`).then((v) => active && (stockVal = v.valeurStock)).catch(() => {})
    api(`/api/businesses/${id}/activity?limit=6`).then((v) => active && (activity = v)).catch(() => {})
    if (compagnie) api(`/api/businesses/${id}/creances`).then((v) => active && (creancesDu = v.reduce((t, f) => t + f.remaining, 0))).catch(() => {})
    else creancesDu = null
    cityTaxDue = 0
    if (canAdmin) api(`/api/businesses/${id}/finance/city-tax`).then((v) => active && (cityTaxDue = v.due)).catch(() => {})
    return () => { active = false }
  })

  const actions = [
    { href: '#/facturation', label: 'Nouvelle facture', icon: FileText, primary: true },
    { href: '#/stock', label: 'Stock', icon: Box },
    { href: '#/statistiques', label: 'Finance', icon: ChartLine },
  ]
</script>

<div style="display:flex; flex-direction:column; gap:14px;">
  <!-- en-tête -->
  <div style="display:flex; align-items:center; justify-content:space-between;">
    <div>
      <div style="font-size:22px; font-weight:700; color:{TEXT};">Bonjour, {displayName}</div>
      <div style="color:{MUTED}; font-size:13.5px; margin-top:2px;">L'essentiel de tes activités d'un coup d'œil.</div>
    </div>
    <span style="background:rgba(232,89,12,0.15); color:{SOFT}; font-size:12px; padding:4px 10px; border-radius:8px;">{GLOBAL_ROLE_LABELS[$me.user.globalRole]}</span>
  </div>

  {#if $currentBusinessId}
    <!-- bandeau business -->
    <div style="display:flex; align-items:center; gap:12px; background:{CARD}; border:{BORDER}; border-radius:12px; padding:12px 14px;">
      <div style="width:40px; height:40px; border-radius:9px; background:{ORANGE}; display:flex; align-items:center; justify-content:center; color:#16110d; overflow:hidden;">
        {#if $currentLogo}<img src={$currentLogo} alt="" style="width:100%; height:100%; object-fit:contain;" />{:else}<Flame size={22} />{/if}
      </div>
      <div style="flex:1; min-width:0;">
        <div style="font-weight:600; color:{TEXT};">{$currentBusiness?.nom}</div>
        <div style="color:{MUTED}; font-size:12px;">{$currentBusiness?.type === 'COMPAGNIE' ? 'Compagnie' : 'Forge'} · business courant</div>
      </div>
      {#if $shift?.open}
        <span style="display:inline-flex; align-items:center; gap:6px; color:#7fd398; font-size:12px; font-weight:600; background:rgba(91,191,115,0.13); padding:5px 11px; border-radius:999px;"><span style="width:7px;height:7px;border-radius:999px;background:{GREEN};"></span>Service en cours</span>
      {/if}
    </div>

    {#if cityTaxDue > 0}
      <!-- rappel taxe ville (admin) -->
      <a href="#/statistiques" style="display:flex; align-items:center; justify-content:space-between; gap:12px; background:rgba(232,160,58,0.10); border:1px solid rgba(232,160,58,0.4); border-radius:12px; padding:11px 14px; text-decoration:none;">
        <span style="color:#e8c06a; font-size:13.5px; font-weight:600;">⚠ Taxe de la ville due : {fmt(cityTaxDue)} septims — à reverser (Finance → Taxe)</span>
        <span style="color:{MUTED}; font-size:12px;">Reverser →</span>
      </a>
    {/if}

    <!-- KPIs -->
    <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(160px,1fr)); gap:12px;">
      {#snippet kpi(label, value, accent)}
        <div style="background:{CARD}; border:1px solid {accent ? 'rgba(232,89,12,0.32)' : 'rgba(255,255,255,0.07)'}; border-radius:12px; padding:14px 16px;">
          <div style="color:{MUTED}; font-size:11px; text-transform:uppercase; letter-spacing:.05em; font-weight:600;">{label}</div>
          <div style="font-size:22px; font-weight:700; margin-top:5px; color:{accent ? SOFT : TEXT};">{value} <span style="font-size:12px; color:{MUTED}; font-weight:500;">septims</span></div>
        </div>
      {/snippet}
      {@render kpi('Encaissé (30 j)', fmt(ov?.caEncaisse), false)}
      {@render kpi('Bénéfice (30 j)', fmt(ov?.benefice), false)}
      {@render kpi('Valeur du stock', fmt(stockVal), false)}
      {#if isCompagnie}{@render kpi('Créances dues', fmt(creancesDu), true)}{/if}
    </div>

    <!-- actions rapides -->
    <div>
      <div style="color:{MUTED}; font-size:11px; text-transform:uppercase; letter-spacing:.06em; font-weight:600; margin-bottom:8px;">Actions rapides</div>
      <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:10px;">
        {#each actions as a (a.href)}
          {@const Icon = a.icon}
          <a href={a.href} style="display:flex; flex-direction:column; gap:8px; text-decoration:none; background:{a.primary ? ORANGE : CARD}; border:{a.primary ? 'none' : BORDER}; border-radius:11px; padding:14px; color:{a.primary ? '#fff' : '#cfc8c2'};">
            <Icon size={20} color={a.primary ? '#fff' : ORANGE} />
            <span style="font-size:13px; font-weight:600;">{a.label}</span>
          </a>
        {/each}
      </div>
    </div>
  {:else}
    <p style="color:{MUTED}; font-size:13.5px;">Sélectionne un business (en haut) pour voir ses indicateurs.</p>
  {/if}

  <!-- deux colonnes -->
  <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(280px,1fr)); gap:12px;">
    <div style="background:{CARD}; border:{BORDER}; border-radius:12px; padding:14px;">
      <div style="font-weight:600; color:{TEXT}; margin-bottom:11px;">Mes business</div>
      {#if $me.memberships.length === 0}
        <p style="color:{MUTED}; font-size:13px;">Tu n'appartiens à aucun business.</p>
      {:else}
        {#each $me.memberships as m (m.businessId)}
          <div style="display:flex; align-items:center; justify-content:space-between; padding:7px 0; border-bottom:1px solid rgba(255,255,255,0.05);">
            <span style="font-size:13px; color:{TEXT};">{m.businessNom} <span style="color:{MUTED}; font-size:11.5px;">· {m.businessType === 'COMPAGNIE' ? 'Compagnie' : 'Forge'}</span></span>
            <span style="background:{m.role === 'ADMIN' ? 'rgba(232,89,12,0.14)' : 'transparent'}; border:{m.role === 'ADMIN' ? 'none' : '1px solid rgba(255,255,255,0.15)'}; color:{m.role === 'ADMIN' ? SOFT : '#cfc8c2'}; font-size:11px; padding:2px 8px; border-radius:6px;">{m.role}</span>
          </div>
        {/each}
      {/if}
    </div>

    <div style="background:{CARD}; border:{BORDER}; border-radius:12px; padding:14px;">
      <div style="font-weight:600; color:{TEXT}; margin-bottom:11px;">Activité récente</div>
      {#if activity.length === 0}
        <p style="color:{MUTED}; font-size:13px;">Aucune activité récente.</p>
      {:else}
        {#each activity as e, i (i)}
          <div style="display:flex; gap:9px; align-items:center; padding:6px 0; font-size:12.5px;">
            <span style="font-size:9.5px; font-weight:700; color:{SOFT}; border:1px solid rgba(232,89,12,0.45); padding:1px 6px; border-radius:5px; flex:none;">{actLabel(e.action)}</span>
            <span style="color:#cfc8c2; flex:1; min-width:0; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">{e.details ?? e.username}</span>
            <span style="color:#6f6862; flex:none;">{time(e.createdAt)}</span>
          </div>
        {/each}
      {/if}
    </div>
  </div>
</div>
