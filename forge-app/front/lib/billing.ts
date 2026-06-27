// Types billing (factures + sessions), alignés sur forge-billing.
export type FactureStatus = "BROUILLON" | "VALIDEE"

export type FactureLine = {
  id: string
  itemId: string
  itemName: string
  quantity: number
  unitPrice: number
  unitCost: number
  lineTotal: number
}

export type Facture = {
  id: string
  numero: number
  status: FactureStatus
  paid: boolean
  clientName: string | null
  totalAmount: number
  totalCost: number
  totalProfit: number
  taxRate: number
  businessShare: number
  workerShare: number
  clientNote: string | null
  internalNote: string | null
  createdAt: string
  validatedAt: string | null
  lines: FactureLine[]
}

export type Session = {
  id: string
  openedAt: string
  closedAt: string | null
  ordersCount: number
  totalSales: number
  totalCost: number
  totalProfit: number
  businessShare: number
  workerShare: number
}

export type ShiftStatus = { open: boolean; session: Session | null }
