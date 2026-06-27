// Types ledger, alignés sur forge-ledger (back).
export type AccountKind = "COFFRE" | "STOCK" | "AUTRE"

export type MovementType =
  | "PRODUCTION"
  | "CONSUMPTION"
  | "SALE"
  | "PURCHASE"
  | "DEPOSIT"
  | "WITHDRAWAL"
  | "TRANSFER"

export type Account = { id: string; name: string; kind: AccountKind }

export type ItemBalance = { itemId: string; itemName: string; balance: number }

/** Ligne de stock agrégée (compte × item) renvoyée par GET …/stock. */
export type StockRow = {
  accountId: string
  accountName: string
  itemId: string
  itemName: string
  quantity: number
}

export type Movement = {
  id: string
  itemId: string
  itemName: string
  quantity: number
  fromAccountId: string | null
  fromAccountName: string | null
  toAccountId: string | null
  toAccountName: string | null
  type: MovementType
  note: string | null
  createdAt: string
}

export const ACCOUNT_KINDS: AccountKind[] = ["STOCK", "COFFRE", "AUTRE"]

export const MOVEMENT_TYPES: MovementType[] = [
  "DEPOSIT",
  "WITHDRAWAL",
  "TRANSFER",
  "PRODUCTION",
  "CONSUMPTION",
  "PURCHASE",
  "SALE",
]
