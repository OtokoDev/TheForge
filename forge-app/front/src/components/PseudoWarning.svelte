<script>
  import { TriangleAlert } from '@lucide/svelte'
  import { me } from '../lib/session.js'

  // Pseudo RP non défini (ou égal au tag Discord) → le joueur apparaît sous son tag.
  let needed = $derived(!$me?.user?.inGameName || $me.user.inGameName === $me.user.username)
</script>

{#if needed}
  <a
    href="#/profil"
    style="display:flex;align-items:center;gap:10px;background:rgba(232,89,12,0.12);border-bottom:1px solid rgba(232,89,12,0.3);color:#f5a06a;font-size:13px;font-weight:500;padding:9px 16px;text-decoration:none;"
  >
    <TriangleAlert size={16} />
    <span>Ton <strong>pseudo en jeu</strong> n'est pas défini — les autres te voient sous ton tag Discord. Clique pour le définir.</span>
  </a>
{/if}
