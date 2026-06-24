"use client"

import { useMemo, useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { ChevronDown, Minus, Plus, ReceiptText, Search, Trash2 } from "lucide-react"
import { toast } from "sonner"
import { ItemIcon, type ProductHandRequired, type ProductItemType } from "@/components/catalogue/item-icon"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { formatMoney } from "@/lib/format"
import { cn } from "@/lib/utils"

type OrderStatus = "EN_ATTENTE" | "EN_FABRICATION" | "PRETE" | "LIVREE" | "ANNULEE"

const statusLabels: Record<OrderStatus, string> = {
  EN_ATTENTE: "En attente",
  EN_FABRICATION: "En fabrication",
  PRETE: "Prête",
  LIVREE: "Livrée",
  ANNULEE: "Annulée",
}

type Product = {
  id: string
  name: string
  category: string
  sellPrice: number
  unitCost: number
  finishedStock: number
  itemType: ProductItemType
  handRequired: ProductHandRequired
}

type ItemSource = "MADE_NOW" | "FROM_STOCK"

type InvoiceItem = Product & {
  quantity: number
  itemSource: ItemSource
}

export function InvoicePanel({ products }: { products: Product[] }) {
  const router = useRouter()
  const [items, setItems] = useState<InvoiceItem[]>([])
  const [status, setStatus] = useState<OrderStatus>("LIVREE")
  const [note, setNote] = useState("")
  const [search, setSearch] = useState("")
  const [openCategories, setOpenCategories] = useState<Record<string, boolean>>({})
  const [isPending, startTransition] = useTransition()

  const totals = useMemo(() => {
    const totalPrice = items.reduce((sum, item) => sum + item.sellPrice * item.quantity, 0)
    const totalCost = items.reduce((sum, item) => sum + item.unitCost * item.quantity, 0)
    const profit = totalPrice - totalCost
    const smithShare = profit * 0.5
    const companyShare = profit * 0.5
    const margin = totalPrice > 0 ? (profit / totalPrice) * 100 : 0
    return { totalPrice, totalCost, profit, smithShare, companyShare, margin }
  }, [items])

  const groupedProducts = useMemo(() => {
    const needle = search.trim().toLocaleLowerCase("fr-FR")
    const filtered = needle
      ? products.filter((product) =>
          `${product.name} ${product.category}`.toLocaleLowerCase("fr-FR").includes(needle),
        )
      : products

    return filtered.reduce<Record<string, Product[]>>((groups, product) => {
      groups[product.category] ??= []
      groups[product.category].push(product)
      return groups
    }, {})
  }, [products, search])
  const hasSearch = search.trim().length > 0
  const stockErrors = useMemo(
    () =>
      items
        .filter((item) => item.itemSource === "FROM_STOCK" && item.quantity > item.finishedStock)
        .map(
          (item) =>
            `Stock insuffisant pour ${item.name} : ${item.quantity} demandé(s), ${item.finishedStock} disponible(s).`,
        ),
    [items],
  )

  function addProduct(product: Product) {
    setItems((current) => {
      const existing = current.find((item) => item.id === product.id)
      if (existing) {
        if (existing.itemSource === "FROM_STOCK" && existing.quantity + 1 > existing.finishedStock) {
          toast.error(
            `Stock insuffisant pour ${existing.name} : ${existing.finishedStock} disponible(s).`,
          )
          return current
        }

        return current.map((item) =>
          item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item,
        )
      }

      return [...current, { ...product, quantity: 1, itemSource: "MADE_NOW" }]
    })
  }

  function updateQuantity(productId: string, quantity: number) {
    setItems((current) =>
      current
        .map((item) => {
          if (item.id !== productId) return item
          if (item.itemSource === "FROM_STOCK" && quantity > item.finishedStock) {
            toast.error(
              `Stock insuffisant pour ${item.name} : ${item.finishedStock} disponible(s).`,
            )
            return item
          }
          return { ...item, quantity }
        })
        .filter((item) => item.quantity > 0),
    )
  }

  function updateItemSource(productId: string, itemSource: ItemSource) {
    setItems((current) =>
      current.map((item) => {
        if (item.id !== productId) return item
        if (itemSource === "FROM_STOCK" && item.quantity > item.finishedStock) {
          toast.error(
            `Stock insuffisant pour ${item.name} : ${item.quantity} demandé(s), ${item.finishedStock} disponible(s).`,
          )
        }
        return { ...item, itemSource }
      }),
    )
  }

  function stockErrorForItem(item: InvoiceItem) {
    if (item.itemSource !== "FROM_STOCK" || item.quantity <= item.finishedStock) return null
    return `Stock insuffisant : ${item.quantity} demandé(s), ${item.finishedStock} disponible(s).`
  }

  function createInvoice() {
    if (stockErrors.length > 0) {
      toast.error(stockErrors[0])
      return
    }

    startTransition(async () => {
      const response = await fetch("/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          status,
          clientNote: note,
          items: items.map((item) => ({
            productId: item.id,
            quantity: item.quantity,
            itemSource: item.itemSource,
          })),
        }),
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        toast.error(payload?.error ?? "Facture impossible")
        return
      }

      setItems([])
      setNote("")
      router.refresh()
      toast.success(`Facture enregistrée (${statusLabels[status]})`)
    })
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[1fr_460px]">
      <div className="flex flex-col gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="relative">
              <Search className="pointer-events-none absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Rechercher un item..."
                className="h-8 w-full rounded-lg border border-input bg-transparent py-1 pl-8 pr-2.5 text-sm outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
              />
            </div>
          </CardContent>
        </Card>

        {Object.entries(groupedProducts).map(([category, categoryProducts]) => {
          const isOpen = openCategories[category] ?? false
          const showItems = isOpen || hasSearch

          return (
            <Card key={category}>
              <CardHeader>
                <button
                  type="button"
                  onClick={() =>
                    setOpenCategories((current) => ({ ...current, [category]: !isOpen }))
                  }
                  className="flex w-full items-center justify-between gap-3 text-left"
                >
                  <CardTitle>{category}</CardTitle>
                  <span className="flex items-center gap-2 text-sm text-muted-foreground">
                    {categoryProducts.length}
                    <ChevronDown
                      data-icon="inline-start"
                      className={showItems ? "rotate-180 transition" : "transition"}
                    />
                  </span>
                </button>
              </CardHeader>
              {showItems ? (
                <CardContent className="grid gap-3 sm:grid-cols-2">
                  {categoryProducts.map((product) => (
                    <button
                      key={product.id}
                      type="button"
                      onClick={() => addProduct(product)}
                      className="rounded-md border bg-card p-4 text-left transition hover:bg-accent"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex min-w-0 gap-3">
                          <ItemIcon
                            itemType={product.itemType}
                            handRequired={product.handRequired}
                          />
                          <div>
                            <p className="font-medium">{product.name}</p>
                            <p className="mt-1 text-sm text-muted-foreground">
                              Coût {formatMoney(product.unitCost)}
                            </p>
                          </div>
                        </div>
                        <Badge variant="secondary">{formatMoney(product.sellPrice)}</Badge>
                      </div>
                    </button>
                  ))}
                </CardContent>
              ) : null}
            </Card>
          )
        })}
      </div>

      <Card className="h-fit">
        <CardHeader>
          <CardTitle>Facture</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <p className="text-sm font-medium">Statut</p>
            <Select
              value={status}
              onValueChange={(value) => setStatus((value ?? "LIVREE") as OrderStatus)}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Object.entries(statusLabels).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">
              Le stock, les ressources et le bénéfice de fermeture sont pris en compte en statut
              Livrée.
            </p>
          </div>

          {items.length === 0 ? (
            <div className="rounded-md border border-dashed p-8 text-center text-sm text-muted-foreground">
              Clique sur les articles à facturer.
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              {items.map((item) => {
                const itemStockError = stockErrorForItem(item)

                return (
                  <div key={item.id} className="rounded-md border bg-muted/20 p-3">
                    <div className="flex items-start gap-3">
                      <ItemIcon itemType={item.itemType} handRequired={item.handRequired} />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <p className="truncate font-medium">{item.name}</p>
                            <p className="text-xs text-muted-foreground">{item.category}</p>
                            <p className="text-xs text-muted-foreground">
                              Marge unité {formatMoney(item.sellPrice - item.unitCost)}
                            </p>
                          </div>
                          <Button
                            size="icon"
                            variant="ghost"
                            onClick={() => updateQuantity(item.id, 0)}
                            aria-label={`Retirer ${item.name}`}
                          >
                            <Trash2 data-icon="inline-start" />
                          </Button>
                        </div>

                        <div className="mt-3 flex flex-col gap-2">
                          <p className="text-xs font-medium">Provenance</p>
                          <div className="grid gap-1 rounded-lg border bg-background p-1 sm:grid-cols-2">
                            <button
                              type="button"
                              aria-pressed={item.itemSource === "MADE_NOW"}
                              onClick={() => updateItemSource(item.id, "MADE_NOW")}
                              className={cn(
                                "min-h-9 rounded-md px-2.5 py-2 text-left text-xs font-medium leading-4 transition",
                                item.itemSource === "MADE_NOW"
                                  ? "bg-primary text-primary-foreground shadow-sm"
                                  : "text-muted-foreground hover:bg-muted hover:text-foreground",
                              )}
                            >
                              Fabriqué
                            </button>
                            <button
                              type="button"
                              aria-pressed={item.itemSource === "FROM_STOCK"}
                              onClick={() => updateItemSource(item.id, "FROM_STOCK")}
                              className={cn(
                                "min-h-9 rounded-md px-2.5 py-2 text-left text-xs font-medium leading-4 transition",
                                item.itemSource === "FROM_STOCK"
                                  ? "bg-primary text-primary-foreground shadow-sm"
                                  : "text-muted-foreground hover:bg-muted hover:text-foreground",
                              )}
                            >
                              Stock
                            </button>
                          </div>
                          {item.itemSource === "FROM_STOCK" ? (
                            <p className="text-xs text-muted-foreground">
                              Stock disponible : {item.finishedStock}
                            </p>
                          ) : null}
                          {itemStockError ? (
                            <p className="text-xs text-destructive">{itemStockError}</p>
                          ) : null}
                        </div>

                        <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
                          <div className="flex items-center gap-2">
                            <Button
                              size="icon"
                              variant="outline"
                              onClick={() => updateQuantity(item.id, item.quantity - 1)}
                            >
                              <Minus data-icon="inline-start" />
                            </Button>
                            <span className="min-w-7 text-center text-sm">{item.quantity}</span>
                            <Button
                              size="icon"
                              variant="outline"
                              onClick={() => updateQuantity(item.id, item.quantity + 1)}
                            >
                              <Plus data-icon="inline-start" />
                            </Button>
                          </div>
                          <p className="text-sm font-semibold">
                            {formatMoney(item.sellPrice * item.quantity)}
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}

          <Textarea
            value={note}
            onChange={(event) => setNote(event.target.value)}
            placeholder="Client, contexte RP, remise éventuelle..."
          />

          <div className="grid gap-3 rounded-md border bg-muted/30 p-4">
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Prix de craft</span>
              <span>{formatMoney(totals.totalCost)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Prix facturé</span>
              <span>{formatMoney(totals.totalPrice)}</span>
            </div>
            <div className="flex justify-between text-lg font-semibold">
              <span>Bénéfice total</span>
              <span>{formatMoney(totals.profit)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Part forgeron</span>
              <span>{formatMoney(totals.smithShare)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Part forge</span>
              <span>{formatMoney(totals.companyShare)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Marge</span>
              <span>{totals.margin.toFixed(1)} %</span>
            </div>
          </div>

          <Button disabled={items.length === 0 || isPending} onClick={createInvoice}>
            <ReceiptText data-icon="inline-start" />
            Enregistrer la facture
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
