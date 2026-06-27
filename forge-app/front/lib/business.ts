export type BusinessType = "FORGE" | "COMPAGNIE"

export type BusinessDto = {
  id: string
  nom: string
  type: BusinessType
  createdAt: string
}
