import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

export function RoleBadge({ role }: { role: "ADMIN" | "GERANT" | "FORGERON" }) {
  return (
    <Badge
      variant="secondary"
      className={cn(
        "border text-xs",
        role === "ADMIN" && "border-red-500/40 bg-red-500/15 text-red-200",
        role === "GERANT" && "border-emerald-500/40 bg-emerald-500/15 text-emerald-200",
        role === "FORGERON" && "border-orange-500/40 bg-orange-500/15 text-orange-200",
      )}
    >
      {role}
    </Badge>
  )
}
