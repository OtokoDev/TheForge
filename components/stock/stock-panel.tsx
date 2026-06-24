"use client"

import { FormEvent, useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { Plus, Save } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger } from "@/components/ui/select"

type Ingredient = {
  id: string
  name: string
  currentStock: number
}

type Product = {
  id: string
  name: string
  finishedStock: number
}

type MovementType = "IN" | "OUT"

export function StockPanel({
  ingredients,
  products,
  canCreateIngredients,
}: {
  ingredients: Ingredient[]
  products: Product[]
  canCreateIngredients: boolean
}) {
  const router = useRouter()
  const [isPending, startTransition] = useTransition()
  const [ingredientId, setIngredientId] = useState(ingredients[0]?.id ?? "")
  const [productId, setProductId] = useState(products[0]?.id ?? "")
  const [movementType, setMovementType] = useState<MovementType>("IN")
  const [productMovementType, setProductMovementType] = useState<MovementType>("IN")
  const [showCreateIngredient, setShowCreateIngredient] = useState(false)
  const selectedIngredient = ingredients.find((ingredient) => ingredient.id === ingredientId)
  const selectedProduct = products.find((product) => product.id === productId)

  function createIngredient(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const payload = Object.fromEntries(form.entries())

    startTransition(async () => {
      const response = await fetch("/api/ingredients", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      })

      if (!response.ok) {
        const error = await response.json().catch(() => null)
        toast.error(error?.error ?? "Création impossible")
        return
      }

      toast.success("Matière créée")
      formElement.reset()
      setShowCreateIngredient(false)
      router.refresh()
    })
  }

  function createMovement(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const quantity = Number(form.get("quantity"))

    if (movementType === "OUT" && selectedIngredient && quantity > selectedIngredient.currentStock) {
      toast.error(`Stock matière insuffisant : ${selectedIngredient.currentStock} disponible(s).`)
      return
    }

    startTransition(async () => {
      const response = await fetch("/api/stock/movement", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          ingredientId,
          type: movementType,
          quantity,
        }),
      })

      if (!response.ok) {
        const error = await response.json().catch(() => null)
        toast.error(error?.error ?? "Mouvement impossible")
        return
      }

      toast.success("Stock mis à jour")
      router.refresh()
    })
  }

  function createProductMovement(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const quantity = Number(form.get("quantity"))

    if (
      productMovementType === "OUT" &&
      selectedProduct &&
      quantity > selectedProduct.finishedStock
    ) {
      toast.error(`Stock produit insuffisant : ${selectedProduct.finishedStock} disponible(s).`)
      return
    }

    startTransition(async () => {
      const response = await fetch("/api/stock/product-movement", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          productId,
          type: productMovementType,
          quantity,
        }),
      })

      if (!response.ok) {
        const error = await response.json().catch(() => null)
        toast.error(error?.error ?? "Mouvement produit impossible")
        return
      }

      toast.success("Stock produit mis à jour")
      router.refresh()
    })
  }

  return (
    <div className="flex flex-col gap-4">
      {canCreateIngredients ? (
        <div className="flex justify-end">
          <Button
            type="button"
            variant={showCreateIngredient ? "secondary" : "default"}
            onClick={() => setShowCreateIngredient((current) => !current)}
          >
            <Plus data-icon="inline-start" />
            Ajouter une matière
          </Button>
        </div>
      ) : null}

      {canCreateIngredients && showCreateIngredient ? (
        <Card>
          <CardHeader>
            <CardTitle>Nouvelle matière</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="grid gap-3" onSubmit={createIngredient}>
              <Label htmlFor="name">Nom</Label>
              <Input id="name" name="name" required />
              <Label htmlFor="unitCost">Coût unitaire</Label>
              <Input id="unitCost" name="unitCost" type="number" min="0" defaultValue="0" />
              <Label htmlFor="currentStock">Stock initial</Label>
              <Input id="currentStock" name="currentStock" type="number" defaultValue="0" />
              <Label htmlFor="alertThreshold">Seuil alerte</Label>
              <Input id="alertThreshold" name="alertThreshold" type="number" defaultValue="5" />
              <Button type="submit" disabled={isPending}>
                <Plus data-icon="inline-start" />
                Ajouter
              </Button>
            </form>
          </CardContent>
        </Card>
      ) : null}

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Mouvement stock matière</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="grid gap-3" onSubmit={createMovement}>
              <Label>Matière</Label>
              <Select value={ingredientId} onValueChange={(value) => setIngredientId(value ?? "")}>
                <SelectTrigger className="w-full">
                  <span className="block min-w-0 flex-1 truncate text-left">
                    {selectedIngredient?.name ?? "Choisir une matière"}
                  </span>
                </SelectTrigger>
                <SelectContent className="min-w-72">
                  {ingredients.map((ingredient) => (
                    <SelectItem key={ingredient.id} value={ingredient.id}>
                      {ingredient.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Label>Type</Label>
              <div className="grid grid-cols-2 gap-2">
                <label className="flex h-9 cursor-pointer items-center gap-2 rounded-lg border border-input px-3 text-sm transition-colors has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                  <input
                    type="checkbox"
                    checked={movementType === "IN"}
                    onChange={() => setMovementType("IN")}
                    className="size-4 accent-primary"
                  />
                  Entrée
                </label>
                <label className="flex h-9 cursor-pointer items-center gap-2 rounded-lg border border-input px-3 text-sm transition-colors has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                  <input
                    type="checkbox"
                    checked={movementType === "OUT"}
                    onChange={() => setMovementType("OUT")}
                    className="size-4 accent-primary"
                  />
                  Sortie
                </label>
              </div>

              <Label htmlFor="quantity">Quantité</Label>
              <Input id="quantity" name="quantity" type="number" min="1" defaultValue="1" />
              {selectedIngredient ? (
                <p className="text-xs text-muted-foreground">
                  Stock actuel : {selectedIngredient.currentStock}
                </p>
              ) : null}
              <Button type="submit" disabled={isPending || !ingredientId}>
                <Save data-icon="inline-start" />
                Enregistrer
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Mouvement produits finis</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="grid gap-3" onSubmit={createProductMovement}>
              <Label>Produit fini</Label>
              <Select value={productId} onValueChange={(value) => setProductId(value ?? "")}>
                <SelectTrigger className="w-full">
                  <span className="block min-w-0 flex-1 truncate text-left">
                    {selectedProduct?.name ?? "Choisir un produit"}
                  </span>
                </SelectTrigger>
                <SelectContent className="min-w-72">
                  {products.map((product) => (
                    <SelectItem key={product.id} value={product.id}>
                      {product.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Label>Type</Label>
              <div className="grid grid-cols-2 gap-2">
                <label className="flex h-9 cursor-pointer items-center gap-2 rounded-lg border border-input px-3 text-sm transition-colors has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                  <input
                    type="checkbox"
                    checked={productMovementType === "IN"}
                    onChange={() => setProductMovementType("IN")}
                    className="size-4 accent-primary"
                  />
                  Entrée
                </label>
                <label className="flex h-9 cursor-pointer items-center gap-2 rounded-lg border border-input px-3 text-sm transition-colors has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                  <input
                    type="checkbox"
                    checked={productMovementType === "OUT"}
                    onChange={() => setProductMovementType("OUT")}
                    className="size-4 accent-primary"
                  />
                  Sortie
                </label>
              </div>

              <Label htmlFor="productQuantity">Quantité</Label>
              <Input id="productQuantity" name="quantity" type="number" min="1" defaultValue="1" />
              {selectedProduct ? (
                <p className="text-xs text-muted-foreground">
                  Stock actuel : {selectedProduct.finishedStock}
                </p>
              ) : null}
              <Button type="submit" disabled={isPending || !productId}>
                <Save data-icon="inline-start" />
                Enregistrer
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
