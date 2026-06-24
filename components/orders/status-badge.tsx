import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

const statusLabels = {
  EN_ATTENTE: "En attente",
  EN_FABRICATION: "En fabrication",
  PRETE: "Prête",
  LIVREE: "Livrée",
  ANNULEE: "Annulée",
}

export function StatusBadge({
  status,
}: {
  status: keyof typeof statusLabels
}) {
  return (
    <Badge
      variant="secondary"
      className={cn(
        "border text-xs",
        status === "EN_ATTENTE" && "border-yellow-500/40 bg-yellow-500/15 text-yellow-100",
        status === "EN_FABRICATION" &&
          "border-orange-500/40 bg-orange-500/15 text-orange-100",
        status === "PRETE" && "border-blue-500/40 bg-blue-500/15 text-blue-100",
        status === "LIVREE" && "border-emerald-500/40 bg-emerald-500/15 text-emerald-100",
        status === "ANNULEE" && "border-red-500/40 bg-red-500/15 text-red-100",
      )}
    >
      {statusLabels[status]}
    </Badge>
  )
}
