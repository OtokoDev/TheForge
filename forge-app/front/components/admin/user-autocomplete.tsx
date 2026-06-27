"use client"

import { useEffect, useRef, useState } from "react"
import { createPortal } from "react-dom"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { api } from "@/lib/api"

export type UserSummary = { id: string; username: string; inGameName: string | null }

function label(u: UserSummary) {
  return u.inGameName ? `${u.inGameName} (@${u.username})` : `@${u.username}`
}

/** Champ de recherche d'utilisateur avec autocomplétion (pseudo Discord ou nom en jeu).
 *  La liste est rendue dans un portal (position fixed) pour ne pas être rognée par
 *  l'overflow des cartes parentes. */
export function UserAutocomplete({
  onSelect,
  placeholder,
}: {
  onSelect: (user: UserSummary | null) => void
  placeholder?: string
}) {
  const [q, setQ] = useState("")
  const [results, setResults] = useState<UserSummary[]>([])
  const [open, setOpen] = useState(false)
  const [picked, setPicked] = useState<UserSummary | null>(null)
  const [rect, setRect] = useState<{ top: number; left: number; width: number } | null>(null)

  const wrapRef = useRef<HTMLDivElement>(null)
  const menuRef = useRef<HTMLUListElement>(null)

  function updateRect() {
    const el = wrapRef.current
    if (!el) return
    const r = el.getBoundingClientRect()
    setRect({ top: r.bottom + 4, left: r.left, width: r.width })
  }

  useEffect(() => {
    if (picked) return
    const term = q.trim()
    if (term.length < 1) {
      setResults([])
      setOpen(false)
      return
    }
    const handle = setTimeout(() => {
      api<UserSummary[]>(`/api/users?q=${encodeURIComponent(term)}`)
        .then((r) => {
          setResults(r)
          updateRect()
          setOpen(true)
        })
        .catch(() => {})
    }, 250)
    return () => clearTimeout(handle)
  }, [q, picked])

  useEffect(() => {
    if (!open) return
    const reposition = () => updateRect()
    const onPointerDown = (e: MouseEvent) => {
      const t = e.target as Node
      if (wrapRef.current?.contains(t) || menuRef.current?.contains(t)) return
      setOpen(false)
    }
    window.addEventListener("scroll", reposition, true)
    window.addEventListener("resize", reposition)
    document.addEventListener("mousedown", onPointerDown)
    return () => {
      window.removeEventListener("scroll", reposition, true)
      window.removeEventListener("resize", reposition)
      document.removeEventListener("mousedown", onPointerDown)
    }
  }, [open])

  function pick(user: UserSummary) {
    setPicked(user)
    setOpen(false)
    setResults([])
    onSelect(user)
  }

  function reset() {
    setPicked(null)
    setQ("")
    onSelect(null)
  }

  if (picked) {
    return (
      <div className="flex items-center gap-2">
        <span className="rounded-lg border border-input px-2.5 py-1 text-sm">{label(picked)}</span>
        <Button variant="ghost" size="sm" onClick={reset}>
          Changer
        </Button>
      </div>
    )
  }

  return (
    <div ref={wrapRef} className="relative">
      <Input
        className="w-64"
        placeholder={placeholder ?? "Rechercher (@pseudo ou nom en jeu)"}
        value={q}
        onChange={(e) => setQ(e.target.value)}
        onFocus={() => {
          if (results.length) {
            updateRect()
            setOpen(true)
          }
        }}
      />
      {open && results.length > 0 && rect
        ? createPortal(
            <ul
              ref={menuRef}
              style={{ position: "fixed", top: rect.top, left: rect.left, width: rect.width, zIndex: 60 }}
              className="max-h-56 overflow-auto rounded-lg border border-border bg-popover text-popover-foreground shadow-md"
            >
              {results.map((u) => (
                <li key={u.id}>
                  <button
                    type="button"
                    className="flex w-full flex-col items-start px-3 py-1.5 text-left text-sm hover:bg-muted"
                    onClick={() => pick(u)}
                  >
                    <span>{u.inGameName ?? u.username}</span>
                    <span className="text-xs text-muted-foreground">@{u.username}</span>
                  </button>
                </li>
              ))}
            </ul>,
            document.body,
          )
        : null}
    </div>
  )
}
