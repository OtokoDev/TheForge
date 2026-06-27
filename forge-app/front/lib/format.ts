export const moneyFormatter = new Intl.NumberFormat("fr-FR", {
  maximumFractionDigits: 2,
  minimumFractionDigits: 0,
})

export function formatMoney(value: number) {
  return `${moneyFormatter.format(value)} septims`
}

export function formatOrderNumber(orderNumber: number) {
  return `#${String(orderNumber).padStart(4, "0")}`
}

export function formatDateTime(value: Date | string) {
  return new Intl.DateTimeFormat("fr-FR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value))
}

export function formatDuration(start: Date | string, end: Date | string = new Date()) {
  const durationMs = new Date(end).getTime() - new Date(start).getTime()
  const totalMinutes = Math.max(0, Math.floor(durationMs / 60000))
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60

  return hours > 0 ? `${hours}h ${minutes}min` : `${minutes}min`
}

export function recipeCost(
  recipe: { quantity: number; ingredient: { unitCost: number } }[],
) {
  return recipe.reduce((total, item) => total + item.quantity * item.ingredient.unitCost, 0)
}
