// Types stats, alignés sur forge-stats.
export type Overview = {
  caEncaisse: number; caEncaissePrev: number
  benefice: number; beneficePrev: number
  nbFactures: number; nbFacturesPrev: number
  panierMoyen: number; panierMoyenPrev: number
  tauxMarge: number
  impaye: number; impayeCount: number
  partBusiness: number; partForgeron: number
  serie: { jour: string; ca: number; benefice: number }[]
}

export type NameValue = { nom: string; valeur: number }
export type ProductStat = { itemId: string; name: string; ca: number; marge: number; qte: number }
export type LossAlert = { name: string; prixRevente: number; cout: number }
export type Products = { top: ProductStat[]; parFamille: NameValue[]; parMateriau: NameValue[]; pertes: LossAlert[] }

export type ForgeronStat = {
  userId: string; username: string; ca: number; benefice: number
  nbFactures: number; minutesService: number; caParHeure: number
}
export type Forgerons = { forgerons: ForgeronStat[] }

export type StockStats = { valeurStock: number; ruptures: NameValue[]; topConsommees: NameValue[] }

export type HeatCell = { dow: number; hour: number; ca: number; count: number }
export type ActivityStats = { heatmap: HeatCell[]; sessions: number; dureeMoyenneMin: number; caParSession: number }

export type DayCreance = { jour: string; credit: number; paiement: number }
export type FarmerStat = { username: string; credited: number; paid: number; remaining: number }
export type CreancesStats = {
  totalDu: number; totalCredit: number; totalPaid: number; ratioPaye: number
  serie: DayCreance[]; topFarmers: FarmerStat[]
}

export type ClientStat = { nom: string; ca: number; nbFactures: number; impaye: number }
export type ClientsStats = { top: ClientStat[]; debiteurs: ClientStat[] }

export type WeekPoint = { semaine: string; ca: number; benefice: number }
export type GlobalStats = {
  totalCa: number; totalBenefice: number
  serie: WeekPoint[]; parBusiness: NameValue[]; topItems: NameValue[]
}

