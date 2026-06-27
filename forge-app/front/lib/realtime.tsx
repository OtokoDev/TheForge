"use client"

import { createContext, useContext, useEffect, useRef } from "react"
import { API_BASE } from "@/lib/api"
import { useCurrentBusiness } from "@/lib/current-business"

type Handler = (type: string) => void
const RealtimeContext = createContext<{ subscribe: (cb: Handler) => () => void } | null>(null)

function wsUrl() {
  const base = API_BASE || `${location.protocol}//${location.host}`
  return base.replace(/^http/, "ws") + "/ws/events"
}

/**
 * Connexion WebSocket unique (cookie JWT envoyé dans l'upgrade, même origine). Réabonne
 * au business courant, ping périodique, reconnexion auto. Les composants écoutent un type
 * d'événement via {@link useRealtime}.
 */
export function RealtimeProvider({ children }: { children: React.ReactNode }) {
  const { currentId } = useCurrentBusiness()
  const handlers = useRef<Set<Handler>>(new Set())
  const wsRef = useRef<WebSocket | null>(null)
  const businessRef = useRef<string | null>(currentId)
  businessRef.current = currentId

  useEffect(() => {
    let closed = false
    let ping: ReturnType<typeof setInterval> | undefined

    function connect() {
      if (closed) return
      let ws: WebSocket
      try { ws = new WebSocket(wsUrl()) } catch { return }
      wsRef.current = ws
      ws.onopen = () => {
        if (businessRef.current) ws.send(`sub:${businessRef.current}`)
        ping = setInterval(() => ws.readyState === WebSocket.OPEN && ws.send("ping"), 30000)
      }
      ws.onmessage = (e) => {
        try {
          const d = JSON.parse(e.data)
          if (d?.type) handlers.current.forEach((h) => h(d.type))
        } catch { /* ignore */ }
      }
      ws.onclose = () => { if (ping) clearInterval(ping); if (!closed) setTimeout(connect, 3000) }
      ws.onerror = () => ws.close()
    }
    connect()
    return () => { closed = true; if (ping) clearInterval(ping); wsRef.current?.close() }
  }, [])

  useEffect(() => {
    const ws = wsRef.current
    if (ws && ws.readyState === WebSocket.OPEN && currentId) ws.send(`sub:${currentId}`)
  }, [currentId])

  const subscribe = (cb: Handler) => {
    handlers.current.add(cb)
    return () => { handlers.current.delete(cb) }
  }
  return <RealtimeContext.Provider value={{ subscribe }}>{children}</RealtimeContext.Provider>
}

/** Exécute {@code onEvent} quand un événement temps réel du type donné arrive. */
export function useRealtime(type: string, onEvent: () => void) {
  const ctx = useContext(RealtimeContext)
  useEffect(() => {
    if (!ctx) return
    return ctx.subscribe((t) => { if (t === type) onEvent() })
  }, [ctx, type, onEvent])
}
