// Créances envers les farmeurs, aligné sur forge-treasury.
export type CreanceFarmer = {
  farmerUserId: string
  farmerUsername: string
  totalCredit: number
  totalPaid: number
  remaining: number
}

export type CreanceEntry = {
  type: "CREDIT" | "PAIEMENT"
  amount: number
  reference: string | null
  username: string
  createdAt: string
}
