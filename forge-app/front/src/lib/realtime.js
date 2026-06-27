// Connexion WebSocket unique (cookie JWT dans l'upgrade, même origine). Réabonnement au
// business courant, ping périodique, reconnexion auto. Singleton module (≠ React Provider).
// Découplé de session.js : l'id business courant est poussé via subscribeBusiness().

const handlers = new Set()
let ws = null
let closed = false
let ping
let currentSub = null

function wsUrl() {
  const base = `${location.protocol}//${location.host}`
  return base.replace(/^http/, 'ws') + '/ws/events'
}

function connect() {
  if (closed) return
  try {
    ws = new WebSocket(wsUrl())
  } catch {
    return
  }
  ws.onopen = () => {
    if (currentSub) ws.send(`sub:${currentSub}`)
    ping = setInterval(() => ws.readyState === WebSocket.OPEN && ws.send('ping'), 30000)
  }
  ws.onmessage = (e) => {
    try {
      const d = JSON.parse(e.data)
      if (d?.type) handlers.forEach((h) => h(d.type))
    } catch {
      /* ignore */
    }
  }
  ws.onclose = () => {
    if (ping) clearInterval(ping)
    if (!closed) setTimeout(connect, 3000)
  }
  ws.onerror = () => ws.close()
}

/** Démarre la connexion (après login). Idempotent. */
export function startRealtime() {
  closed = false
  if (!ws) connect()
}

/** Mémorise et (ré)abonne au flux d'un business. */
export function subscribeBusiness(id) {
  currentSub = id
  if (ws && ws.readyState === WebSocket.OPEN && id) ws.send(`sub:${id}`)
}

/** Exécute {@code cb} quand un événement du type donné arrive. Retourne un désabonnement. */
export function onRealtime(type, cb) {
  const h = (t) => {
    if (t === type) cb()
  }
  handlers.add(h)
  return () => handlers.delete(h)
}
