"use client"

import { ShoppingCart } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"

type CartItem = {
  productId: string
  name: string
  price: number
  quantity: number
}

export function AddToCartButton({
  product,
}: {
  product: { id: string; name: string; sellPrice: number }
}) {
  return (
    <Button
      size="sm"
      onClick={() => {
        const current = JSON.parse(localStorage.getItem("forge-rp-cart") ?? "[]") as CartItem[]
        const existing = current.find((item) => item.productId === product.id)
        const next = existing
          ? current.map((item) =>
              item.productId === product.id ? { ...item, quantity: item.quantity + 1 } : item,
            )
          : [
              ...current,
              {
                productId: product.id,
                name: product.name,
                price: product.sellPrice,
                quantity: 1,
              },
            ]

        localStorage.setItem("forge-rp-cart", JSON.stringify(next))
        toast.success(`${product.name} ajouté au panier`)
      }}
    >
      <ShoppingCart data-icon="inline-start" />
      Ajouter
    </Button>
  )
}
