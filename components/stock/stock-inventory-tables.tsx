"use client"

import { useMemo, useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { AlertTriangle, ArrowDown, ArrowDownUp, ArrowUp, Save, Trash2 } from "lucide-react"
import { toast } from "sonner"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatMoney } from "@/lib/format"

type StockIngredient = {
  id: string
  name: string
  currentStock: number
  alertThreshold: number
  unitCost: number
}

type StockProduct = {
  id: string
  name: string
  category: string
  finishedStock: number
  sellPrice: number
}

type DeleteTarget = {
  kind: "ingredient" | "product"
  id: string
  name: string
}

type SortDirection = "asc" | "desc"
type IngredientSortKey = "name" | "currentStock" | "alertThreshold" | "unitCost"
type ProductSortKey = "name" | "category" | "finishedStock" | "sellPrice"

type SortState<T extends string> = {
  key: T
  direction: SortDirection
}

const ingredientSortTypes: Record<IngredientSortKey, "text" | "number"> = {
  name: "text",
  currentStock: "number",
  alertThreshold: "number",
  unitCost: "number",
}

const productSortTypes: Record<ProductSortKey, "text" | "number"> = {
  name: "text",
  category: "text",
  finishedStock: "number",
  sellPrice: "number",
}

function defaultDirection(type: "text" | "number") {
  return type === "number" ? "desc" : "asc"
}

function compareText(a: string, b: string, direction: SortDirection) {
  const result = a.localeCompare(b, "fr-FR", { sensitivity: "base" })
  return direction === "asc" ? result : -result
}

function compareNumber(a: number, b: number, direction: SortDirection) {
  const result = a - b
  return direction === "asc" ? result : -result
}

function SortHeader<T extends string>({
  label,
  sortKey,
  sort,
  onSort,
}: {
  label: string
  sortKey: T
  sort: SortState<T>
  onSort: (key: T) => void
}) {
  const active = sort.key === sortKey
  const Icon = active ? (sort.direction === "asc" ? ArrowUp : ArrowDown) : ArrowDownUp

  return (
    <Button
      type="button"
      variant="ghost"
      size="sm"
      className="h-7 px-2"
      onClick={() => onSort(sortKey)}
      aria-sort={active ? (sort.direction === "asc" ? "ascending" : "descending") : "none"}
    >
      <Icon data-icon="inline-start" />
      {label}
    </Button>
  )
}

export function StockInventoryTables({
  ingredients,
  products,
  canDelete,
  canEditIngredientSettings,
}: {
  ingredients: StockIngredient[]
  products: StockProduct[]
  canDelete: boolean
  canEditIngredientSettings: boolean
}) {
  const router = useRouter()
  const [deleteTarget, setDeleteTarget] = useState<DeleteTarget | null>(null)
  const [ingredientSort, setIngredientSort] = useState<SortState<IngredientSortKey>>({
    key: "name",
    direction: "asc",
  })
  const [productSort, setProductSort] = useState<SortState<ProductSortKey>>({
    key: "name",
    direction: "asc",
  })
  const [ingredientDrafts, setIngredientDrafts] = useState(() =>
    Object.fromEntries(
      ingredients.map((item) => [
        item.id,
        {
          alertThreshold: String(item.alertThreshold),
          unitCost: String(item.unitCost),
        },
      ]),
    ),
  )
  const [isPending, startTransition] = useTransition()
  const sortedIngredients = useMemo(() => {
    return [...ingredients].sort((a, b) => {
      if (ingredientSort.key === "name") {
        return compareText(a.name, b.name, ingredientSort.direction)
      }

      return compareNumber(a[ingredientSort.key], b[ingredientSort.key], ingredientSort.direction)
    })
  }, [ingredients, ingredientSort])
  const sortedProducts = useMemo(() => {
    return [...products].sort((a, b) => {
      if (productSort.key === "name" || productSort.key === "category") {
        return compareText(a[productSort.key], b[productSort.key], productSort.direction)
      }

      return compareNumber(a[productSort.key], b[productSort.key], productSort.direction)
    })
  }, [products, productSort])

  function updateIngredientSort(key: IngredientSortKey) {
    setIngredientSort((current) =>
      current.key === key
        ? { key, direction: current.direction === "asc" ? "desc" : "asc" }
        : { key, direction: defaultDirection(ingredientSortTypes[key]) },
    )
  }

  function updateProductSort(key: ProductSortKey) {
    setProductSort((current) =>
      current.key === key
        ? { key, direction: current.direction === "asc" ? "desc" : "asc" }
        : { key, direction: defaultDirection(productSortTypes[key]) },
    )
  }

  function updateIngredientDraft(
    id: string,
    field: "alertThreshold" | "unitCost",
    value: string,
  ) {
    setIngredientDrafts((current) => ({
      ...current,
      [id]: {
        alertThreshold: current[id]?.alertThreshold ?? "",
        unitCost: current[id]?.unitCost ?? "",
        [field]: value,
      },
    }))
  }

  function saveIngredientSettings(item: StockIngredient) {
    const draft = ingredientDrafts[item.id]
    const alertThreshold = Number(draft?.alertThreshold)
    const unitCost = Number(draft?.unitCost)

    if (!Number.isInteger(alertThreshold) || alertThreshold < 0) {
      toast.error("Le seuil doit être un nombre entier positif")
      return
    }
    if (!Number.isFinite(unitCost) || unitCost < 0) {
      toast.error("Le coût doit être un nombre positif")
      return
    }

    startTransition(async () => {
      const response = await fetch(`/api/ingredients/${item.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ alertThreshold, unitCost }),
      })

      if (!response.ok) {
        const error = await response.json().catch(() => null)
        toast.error(error?.error ?? "Mise à jour impossible")
        return
      }

      toast.success(`Paramètres ${item.name} mis à jour`)
      router.refresh()
    })
  }

  function confirmDelete() {
    if (!deleteTarget) return

    startTransition(async () => {
      const endpoint =
        deleteTarget.kind === "ingredient"
          ? `/api/ingredients/${deleteTarget.id}`
          : `/api/products/${deleteTarget.id}`
      const response = await fetch(endpoint, { method: "DELETE" })

      if (!response.ok) {
        const error = await response.json().catch(() => null)
        toast.error(error?.error ?? "Suppression impossible")
        return
      }

      toast.success(deleteTarget.kind === "ingredient" ? "Matière supprimée" : "Produit supprimé")
      setDeleteTarget(null)
      router.refresh()
    })
  }

  return (
    <>
      <div className="grid gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Matières premières</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>
                    <SortHeader
                      label="Nom"
                      sortKey="name"
                      sort={ingredientSort}
                      onSort={updateIngredientSort}
                    />
                  </TableHead>
                  <TableHead>
                    <SortHeader
                      label="Stock"
                      sortKey="currentStock"
                      sort={ingredientSort}
                      onSort={updateIngredientSort}
                    />
                  </TableHead>
                  <TableHead>
                    <SortHeader
                      label="Seuil"
                      sortKey="alertThreshold"
                      sort={ingredientSort}
                      onSort={updateIngredientSort}
                    />
                  </TableHead>
                  <TableHead>
                    <SortHeader
                      label="Coût"
                      sortKey="unitCost"
                      sort={ingredientSort}
                      onSort={updateIngredientSort}
                    />
                  </TableHead>
                  {canEditIngredientSettings || canDelete ? (
                    <TableHead className="w-20" />
                  ) : null}
                </TableRow>
              </TableHeader>
              <TableBody>
                {sortedIngredients.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell>{item.name}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        {item.currentStock}
                        {item.currentStock <= item.alertThreshold ? (
                          <Badge variant="secondary" className="border-yellow-500/40 bg-yellow-500/15">
                            <AlertTriangle data-icon="inline-start" />
                            bas
                          </Badge>
                        ) : null}
                      </div>
                    </TableCell>
                    <TableCell>
                      {canEditIngredientSettings ? (
                        <Input
                          type="number"
                          min="0"
                          step="1"
                          value={ingredientDrafts[item.id]?.alertThreshold ?? ""}
                          onChange={(event) =>
                            updateIngredientDraft(item.id, "alertThreshold", event.target.value)
                          }
                          className="w-20"
                          aria-label={`Seuil alerte ${item.name}`}
                        />
                      ) : (
                        item.alertThreshold
                      )}
                    </TableCell>
                    <TableCell>
                      {canEditIngredientSettings ? (
                        <Input
                          type="number"
                          min="0"
                          step="0.01"
                          value={ingredientDrafts[item.id]?.unitCost ?? ""}
                          onChange={(event) =>
                            updateIngredientDraft(item.id, "unitCost", event.target.value)
                          }
                          className="w-24"
                          aria-label={`Coût ${item.name}`}
                        />
                      ) : (
                        formatMoney(item.unitCost)
                      )}
                    </TableCell>
                    {canEditIngredientSettings || canDelete ? (
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          {canEditIngredientSettings ? (
                            <Button
                              type="button"
                              variant="outline"
                              size="icon-sm"
                              disabled={isPending}
                              title={`Enregistrer ${item.name}`}
                              onClick={() => saveIngredientSettings(item)}
                            >
                              <Save data-icon="inline-start" />
                            </Button>
                          ) : null}
                          {canDelete ? (
                            <Button
                              type="button"
                              variant="destructive"
                              size="icon-sm"
                              disabled={isPending}
                              title={`Supprimer ${item.name}`}
                              onClick={() =>
                                setDeleteTarget({
                                  kind: "ingredient",
                                  id: item.id,
                                  name: item.name,
                                })
                              }
                            >
                              <Trash2 />
                            </Button>
                          ) : null}
                        </div>
                      </TableCell>
                    ) : null}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Produits finis</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>
                    <SortHeader
                      label="Produit"
                      sortKey="name"
                      sort={productSort}
                      onSort={updateProductSort}
                    />
                  </TableHead>
                  <TableHead>
                    <SortHeader
                      label="Catégorie"
                      sortKey="category"
                      sort={productSort}
                      onSort={updateProductSort}
                    />
                  </TableHead>
                  <TableHead>
                    <SortHeader
                      label="Stock"
                      sortKey="finishedStock"
                      sort={productSort}
                      onSort={updateProductSort}
                    />
                  </TableHead>
                  <TableHead>
                    <SortHeader
                      label="Prix"
                      sortKey="sellPrice"
                      sort={productSort}
                      onSort={updateProductSort}
                    />
                  </TableHead>
                  {canDelete ? <TableHead className="w-10" /> : null}
                </TableRow>
              </TableHeader>
              <TableBody>
                {sortedProducts.map((product) => (
                  <TableRow key={product.id}>
                    <TableCell>{product.name}</TableCell>
                    <TableCell>{product.category}</TableCell>
                    <TableCell>{product.finishedStock}</TableCell>
                    <TableCell>{formatMoney(product.sellPrice)}</TableCell>
                    {canDelete ? (
                      <TableCell className="text-right">
                        <Button
                          type="button"
                          variant="destructive"
                          size="icon-sm"
                          disabled={isPending}
                          title={`Supprimer ${product.name}`}
                          onClick={() =>
                            setDeleteTarget({ kind: "product", id: product.id, name: product.name })
                          }
                        >
                          <Trash2 />
                        </Button>
                      </TableCell>
                    ) : null}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>

      <Dialog
        open={Boolean(deleteTarget)}
        onOpenChange={(open) => {
          if (!open && !isPending) setDeleteTarget(null)
        }}
      >
        <DialogContent showCloseButton={!isPending}>
          <DialogHeader>
            <DialogTitle>Confirmer la suppression</DialogTitle>
            <DialogDescription>
              {deleteTarget ? (
                <>
                  Tu vas supprimer{" "}
                  <span className="font-medium text-foreground">{deleteTarget.name}</span> de la
                  liste active. Cette action masquera l&apos;élément sans casser l&apos;historique.
                </>
              ) : null}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              disabled={isPending}
              onClick={() => setDeleteTarget(null)}
            >
              Annuler
            </Button>
            <Button type="button" variant="destructive" disabled={isPending} onClick={confirmDelete}>
              <Trash2 data-icon="inline-start" />
              Supprimer
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}
