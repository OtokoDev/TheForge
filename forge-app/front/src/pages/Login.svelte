<script>
  import { Hammer, Pickaxe, Coins, Store } from '@lucide/svelte'
  import { LOGIN_URL } from '../lib/api.js'

  const chips = [
    { icon: Pickaxe, label: 'Minerais' },
    { icon: Coins, label: 'Septims' },
    { icon: Store, label: 'Échoppe' },
  ]
</script>

<main style="position:relative; min-height:100vh; display:flex; align-items:center; justify-content:center; background:#0a0806; overflow:hidden; font-family:system-ui,-apple-system,'Segoe UI',sans-serif;">
  <img src="/markarth.png" alt="" style="position:absolute; inset:0; width:100%; height:100%; object-fit:cover; object-position:center;" />
  <div style="position:absolute; inset:0; background:rgba(10,8,6,0.58);"></div>

  <div style="position:relative; width:100%; max-width:400px; margin:0 16px; text-align:center; background:rgba(20,17,14,0.74); backdrop-filter:blur(8px); -webkit-backdrop-filter:blur(8px); border:1px solid rgba(255,255,255,0.1); border-radius:18px; padding:34px 30px; box-shadow:0 30px 80px rgba(0,0,0,0.5);">

    <div style="width:60px; height:60px; border-radius:15px; background:#E8590C; display:inline-flex; align-items:center; justify-content:center; color:#fff; margin-bottom:16px;"><Hammer size={32} /></div>

    <div style="font-size:28px; font-weight:700; color:#F4F1EE; letter-spacing:-0.01em;">Forge de Markarth</div>
    <div style="color:#b8afa4; font-size:14px; line-height:1.55; margin-top:9px;">Tiens l'échoppe et la forge de la cité — minerais, lingots et septims, du marteau à la caisse.</div>

    <div style="display:flex; justify-content:center; gap:8px; flex-wrap:wrap; margin:22px 0 24px;">
      {#each chips as c (c.label)}
        {@const Icon = c.icon}
        <span style="display:inline-flex; align-items:center; gap:6px; background:rgba(28,26,24,0.8); border:1px solid rgba(255,255,255,0.1); color:#d8d0c7; font-size:12.5px; padding:6px 11px; border-radius:999px;"><Icon size={15} color="#E8590C" />{c.label}</span>
      {/each}
    </div>

    <button
      onclick={() => (window.location.href = LOGIN_URL)}
      style="width:100%; display:flex; align-items:center; justify-content:center; gap:10px; background:#5865F2; border:none; color:#fff; font-size:15px; font-weight:600; padding:14px; border-radius:11px; cursor:pointer;"
    >
      <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M20.317 4.369a19.79 19.79 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128c.126-.094.252-.192.372-.291a.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.009c.12.099.246.198.373.292a.077.077 0 0 1-.006.127 12.3 12.3 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.84 19.84 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.331c-1.183 0-2.157-1.086-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.332-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.086-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.332-.946 2.418-2.157 2.418z" /></svg>
      Se connecter avec Discord
    </button>
    <div style="color:#8a8076; font-size:12px; margin-top:16px;">Accès réservé aux membres du serveur.</div>
  </div>
</main>
