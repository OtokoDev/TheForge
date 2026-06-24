import type { Order, OrderItem, OrderResourceConsumption, Product, User } from "@prisma/client"

export type DeliveredOrderWithDetails = Order & {
  user: User
  items: (OrderItem & { product: Product })[]
  consumptions: OrderResourceConsumption[]
}

export function dayKey(value: Date) {
  return value.toISOString().slice(0, 10)
}

export function sumByDay(
  orders: DeliveredOrderWithDetails[],
  selector: (order: DeliveredOrderWithDetails) => number,
) {
  const grouped = new Map<string, number>()
  for (const order of orders) {
    const key = dayKey(order.createdAt)
    grouped.set(key, (grouped.get(key) ?? 0) + selector(order))
  }

  return Array.from(grouped.entries())
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([label, value]) => ({ label, value }))
}

export function topSoldItems(orders: DeliveredOrderWithDetails[]) {
  const grouped = new Map<string, number>()
  for (const order of orders) {
    for (const item of order.items) {
      grouped.set(item.product.name, (grouped.get(item.product.name) ?? 0) + item.quantity)
    }
  }

  return Array.from(grouped.entries())
    .map(([label, value]) => ({ label, value }))
    .sort((left, right) => right.value - left.value)
}

export function topConsumedResources(
  consumptions: (OrderResourceConsumption & { ingredient?: { name: string } })[],
) {
  const grouped = new Map<string, number>()
  for (const consumption of consumptions) {
    const label = consumption.ingredient?.name ?? consumption.ingredientId
    grouped.set(label, (grouped.get(label) ?? 0) + consumption.quantity)
  }

  return Array.from(grouped.entries())
    .map(([label, value]) => ({ label, value }))
    .sort((left, right) => right.value - left.value)
}

export function workedMinutes(sessions: { openedAt: Date; closedAt: Date | null }[]) {
  return sessions.reduce((sum, session) => {
    const end = session.closedAt ?? new Date()
    return sum + Math.max(0, end.getTime() - session.openedAt.getTime()) / 60000
  }, 0)
}

export function formatWorkedMinutes(minutes: number) {
  const totalMinutes = Math.floor(minutes)
  const hours = Math.floor(totalMinutes / 60)
  const rest = totalMinutes % 60
  return hours > 0 ? `${hours}h ${rest}min` : `${rest}min`
}
