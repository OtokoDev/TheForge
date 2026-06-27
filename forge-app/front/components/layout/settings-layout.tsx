"use client"

import { cn } from "@/lib/utils"

export type SettingsTab = { key: string; label: string }

/** Mise en page « config » à la Nexis : titre + sous-menu vertical à gauche, contenu à droite. */
export function SettingsLayout({ title, subtitle, tabs, active, onSelect, children }: {
  title: string
  subtitle?: string
  tabs: SettingsTab[]
  active: string
  onSelect: (key: string) => void
  children: React.ReactNode
}) {
  return (
    <div>
      <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
      {subtitle ? <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p> : null}
      <div className="mt-5 flex flex-col gap-6 md:flex-row">
        <nav className="flex shrink-0 flex-row flex-wrap gap-1 md:w-56 md:flex-col">
          {tabs.map((t) => (
            <button
              key={t.key}
              onClick={() => onSelect(t.key)}
              className={cn(
                "rounded-md px-3 py-2 text-left text-sm transition",
                active === t.key
                  ? "bg-sidebar-accent font-medium text-sidebar-accent-foreground"
                  : "text-muted-foreground hover:bg-muted hover:text-foreground",
              )}
            >
              {t.label}
            </button>
          ))}
        </nav>
        <div className="min-w-0 flex-1">{children}</div>
      </div>
    </div>
  )
}
