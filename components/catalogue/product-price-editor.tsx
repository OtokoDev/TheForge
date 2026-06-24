"use client"

import { useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { Save } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { formatMoney } from "@/lib/format"

export function ProductPriceEditor({
  productId,
  productName,
  initialSellPrice,
  unitCost,
  canEdit,
}: {
  productId: string
  productName: string
  initialSellPrice: number
  unitCost: number
  canEdit: boolean
}) {
  const router = useRouter()
  const [price, setPrice] = useState(String(initialSellPrice))
  const [isPending, startTransition] = useTransition()
  const parsedPrice = Number(price.replace(",", "."))
  const sellPrice = Number.isFinite(parsedPrice) ? parsedPrice : 0

  function savePrice() {
    startTransition(async () => {
      const response = await fetch(`/api/products/${productId}/price`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sellPrice }),
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        toast.error(payload?.error ?? "Prix impossible à modifier")
        return
      }

      toast.success(`Prix ${productName} modifié`)
      router.refresh()
    })
  }

  if (!canEdit) {
    return (
      <div>
        <p className="text-xs text-muted-foreground">Prix de vente</p>
        <p className="text-2xl font-semibold">{formatMoney(initialSellPrice)}</p>
        <p className="text-xs text-muted-foreground">
          Marge {formatMoney(initialSellPrice - unitCost)}
        </p>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-2">
      <p className="text-xs text-muted-foreground">Prix de vente</p>
      <div className="flex items-center gap-2">
        <Input
          type="number"
          min="0"
          step="0.01"
          value={price}
          onChange={(event) => setPrice(event.target.value)}
        />
        <Button size="icon" variant="outline" disabled={isPending} onClick={savePrice}>
          <Save data-icon="inline-start" />
        </Button>
      </div>
      <p className="text-xs text-muted-foreground">Coût fabrication {formatMoney(unitCost)}</p>
      <p className="text-xs text-muted-foreground">Marge {formatMoney(sellPrice - unitCost)}</p>
    </div>
  )
}
