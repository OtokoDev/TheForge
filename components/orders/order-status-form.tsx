"use client"

import { useRouter } from "next/navigation"
import { useState, useTransition } from "react"
import { Save } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"

const statuses = ["EN_ATTENTE", "EN_FABRICATION", "PRETE", "LIVREE", "ANNULEE"] as const

export function OrderStatusForm({
  orderId,
  status,
  internalNote,
}: {
  orderId: string
  status: (typeof statuses)[number]
  internalNote?: string | null
}) {
  const router = useRouter()
  const [nextStatus, setNextStatus] = useState(status)
  const [note, setNote] = useState(internalNote ?? "")
  const [isPending, startTransition] = useTransition()

  function save() {
    startTransition(async () => {
      const response = await fetch(`/api/orders/${orderId}/status`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: nextStatus, internalNote: note }),
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        toast.error(payload?.error ?? "Mise à jour impossible")
        return
      }

      toast.success("Commande mise à jour")
      router.refresh()
    })
  }

  return (
    <div className="flex flex-col gap-3">
      <Select value={nextStatus} onValueChange={(value) => setNextStatus(value as typeof nextStatus)}>
        <SelectTrigger>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {statuses.map((item) => (
            <SelectItem key={item} value={item}>
              {item}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Textarea
        value={note}
        onChange={(event) => setNote(event.target.value)}
        placeholder="Note interne non visible par le client"
      />
      <Button disabled={isPending} onClick={save}>
        <Save data-icon="inline-start" />
        Enregistrer
      </Button>
    </div>
  )
}
