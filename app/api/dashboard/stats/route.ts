import { NextResponse } from "next/server"
import { prisma } from "@/lib/prisma"
import { guardApi } from "@/lib/api"

export const dynamic = "force-dynamic"

export async function GET() {
  const { response } = await guardApi("FORGERON")
  if (response) return response

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const [
    shopSession,
    pendingOrders,
    craftingOrders,
    lowStock,
    latestOrders,
    deliveredToday,
  ] = await Promise.all([
    prisma.shopSession.findFirst({
      where: { closedAt: null },
      include: { user: { select: { username: true, avatar: true } } },
    }),
    prisma.order.count({ where: { status: "EN_ATTENTE" } }),
    prisma.order.count({ where: { status: "EN_FABRICATION" } }),
    prisma.ingredient.findMany({
      where: { isActive: true, currentStock: { lte: prisma.ingredient.fields.alertThreshold } },
      orderBy: { currentStock: "asc" },
      take: 8,
    }),
    prisma.order.findMany({
      take: 5,
      include: { user: true, items: { include: { product: true } } },
      orderBy: { createdAt: "desc" },
    }),
    prisma.order.findMany({
      where: { status: "LIVREE", deliveredAt: { gte: today } },
      include: { items: true },
    }),
  ])

  const todaySales = deliveredToday.reduce((total, order) => total + order.totalPrice, 0)
  const todayProfit = deliveredToday.reduce((total, order) => total + order.totalProfit, 0)
  const todayCompanyShare = deliveredToday.reduce((total, order) => total + order.companyShare, 0)
  const todaySmithShare = deliveredToday.reduce((total, order) => total + order.smithShare, 0)

  return NextResponse.json({
    shopSession,
    pendingOrders,
    craftingOrders,
    lowStock,
    latestOrders,
    todaySales,
    todayProfit,
    todayCompanyShare,
    todaySmithShare,
  })
}
