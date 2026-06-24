"use client"

import { useEffect, useMemo, useState, useTransition } from "react"
import { Minus, Plus, Send, Trash2 } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Textarea } from "@/components/ui/textarea"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatMoney } from "@/lib/format"

type CartItem = {
  productId: string
  name: string
  price: number
  quantity: number
}

export function CartPanel() {
  const [items, setItems] = useState<CartItem[]>([])
  const [clientNote, setClientNote] = useState("")
  const [isPending, startTransition] = useTransition()

  useEffect(() => {
    setItems(JSON.parse(localStorage.getItem("forge-rp-cart") ?? "[]") as CartItem[])
  }, [])

  useEffect(() => {
    localStorage.setItem("forge-rp-cart", JSON.stringify(items))
  }, [items])

  const total = useMemo(
    () => items.reduce((sum, item) => sum + item.price * item.quantity, 0),
    [items],
  )

  function updateQuantity(productId: string, quantity: number) {
    setItems((current) =>
      current
        .map((item) => (item.productId === productId ? { ...item, quantity } : item))
        .filter((item) => item.quantity > 0),
    )
  }

  function submitOrder() {
    startTransition(async () => {
      const response = await fetch("/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          items: items.map((item) => ({
            productId: item.productId,
            quantity: item.quantity,
          })),
          clientNote,
        }),
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        toast.error(payload?.error ?? "Commande impossible")
        return
      }

      setItems([])
      setClientNote("")
      toast.success("Commande confirmée")
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Panier</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-5">
        {items.length === 0 ? (
          <div className="rounded-md border border-dashed p-8 text-center text-sm text-muted-foreground">
            Le panier est vide.
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Article</TableHead>
                <TableHead>Quantité</TableHead>
                <TableHead className="text-right">Sous-total</TableHead>
                <TableHead className="w-12" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((item) => (
                <TableRow key={item.productId}>
                  <TableCell>
                    <p className="font-medium">{item.name}</p>
                    <p className="text-xs text-muted-foreground">{formatMoney(item.price)}</p>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <Button
                        size="icon"
                        variant="outline"
                        onClick={() => updateQuantity(item.productId, item.quantity - 1)}
                      >
                        <Minus data-icon="inline-start" />
                      </Button>
                      <span className="w-8 text-center text-sm">{item.quantity}</span>
                      <Button
                        size="icon"
                        variant="outline"
                        onClick={() => updateQuantity(item.productId, item.quantity + 1)}
                      >
                        <Plus data-icon="inline-start" />
                      </Button>
                    </div>
                  </TableCell>
                  <TableCell className="text-right">
                    {formatMoney(item.price * item.quantity)}
                  </TableCell>
                  <TableCell>
                    <Button
                      size="icon"
                      variant="ghost"
                      onClick={() => updateQuantity(item.productId, 0)}
                    >
                      <Trash2 data-icon="inline-start" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <Textarea
          value={clientNote}
          onChange={(event) => setClientNote(event.target.value)}
          placeholder="Note client, horaire de livraison, détail RP..."
        />

        <div className="flex items-center justify-between rounded-md border bg-muted/30 p-4">
          <span className="text-sm text-muted-foreground">Total</span>
          <span className="text-xl font-semibold">{formatMoney(total)}</span>
        </div>

        <Button disabled={items.length === 0 || isPending} onClick={submitOrder}>
          <Send data-icon="inline-start" />
          Confirmer la commande
        </Button>
      </CardContent>
    </Card>
  )
}
