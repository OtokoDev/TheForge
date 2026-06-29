// Septim insécable : montants toujours en entiers (arrondi).
const moneyFormatter = new Intl.NumberFormat('fr-FR', {
  maximumFractionDigits: 0,
  minimumFractionDigits: 0,
})

export function formatMoney(value) {
  return `${moneyFormatter.format(value ?? 0)} septims`
}

export function formatOrderNumber(n) {
  return `#${String(n).padStart(4, '0')}`
}

export function formatDateTime(value) {
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
}

export function formatDate(value) {
  return new Date(value).toLocaleDateString('fr-FR')
}

export function formatDuration(start, end = new Date()) {
  const ms = new Date(end).getTime() - new Date(start).getTime()
  const total = Math.max(0, Math.floor(ms / 60000))
  const h = Math.floor(total / 60)
  const m = total % 60
  return h > 0 ? `${h}h ${m}min` : `${m}min`
}
