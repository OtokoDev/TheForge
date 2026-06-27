// Types catalogue, alignés sur forge-catalog (back).
export type HandRequired = "ONE" | "TWO"

// Dimension de classement configurable par SYSTEM (famille OU matériau).
export type Taxon = { id: string; nom: string; ordre: number; couleur: string | null; version: number }
export type Family = Taxon
export type Material = Taxon

export type Item = {
  id: string
  name: string
  familyId: string | null
  familyName: string | null
  familyColor: string | null
  materialId: string | null
  materialName: string | null
  materialColor: string | null
  handRequired: HandRequired | null
  active: boolean
  system: boolean
  hasRecipe: boolean
  version: number
}

export type RecipeComponentLine = {
  componentItemId: string
  componentName: string
  quantity: number
}

// Produit d'un business : valeur (coût, matières) + prix de revente. validFrom null = septime.
export type Product = {
  itemId: string
  itemName: string
  hasRecipe: boolean
  valeur: number | null
  prixRevente: number | null
  validFrom: string | null
  version: number
}

export type ProductHistory = {
  valeur: number | null
  prixRevente: number | null
  validFrom: string
  validTo: string | null
}
