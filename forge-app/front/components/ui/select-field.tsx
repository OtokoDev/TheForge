"use client"

import { ChevronDown } from "lucide-react"
import { cn } from "@/lib/utils"

export type SelectOption = { value: string; label: string }

/**
 * Liste déroulante réutilisable. S'appuie sur un <select> natif mais force
 * color-scheme: dark → la liste native s'affiche en sombre (corrige le texte
 * blanc sur fond blanc des options du thème sombre). Flèche custom via appearance-none.
 */
export function SelectField({
  value,
  onChange,
  options,
  className,
  ariaLabel,
  disabled,
}: {
  value: string
  onChange: (value: string) => void
  options: SelectOption[]
  className?: string
  ariaLabel?: string
  disabled?: boolean
}) {
  return (
    <div className={cn("relative inline-flex items-center", className)}>
      <select
        aria-label={ariaLabel}
        disabled={disabled}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        style={{ colorScheme: "dark" }}
        className="h-8 appearance-none rounded-lg border border-input bg-transparent py-1 pr-8 pl-2.5 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-input/30"
      >
        {options.map((o) => (
          <option key={o.value} value={o.value} className="bg-popover text-popover-foreground">
            {o.label}
          </option>
        ))}
      </select>
      <ChevronDown className="pointer-events-none absolute right-2 size-4 text-muted-foreground" />
    </div>
  )
}
