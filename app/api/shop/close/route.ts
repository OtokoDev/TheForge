import { NextResponse } from "next/server"
import { guardApi, jsonError } from "@/lib/api"
import { prisma } from "@/lib/prisma"
import { webhookShopClose } from "@/lib/webhooks"

export const dynamic = "force-dynamic"

export async function POST() {
  const { response, session } = await guardApi("FORGERON")
  if (response) return response

  const activeSession = await prisma.shopSession.findFirst({
    where: { closedAt: null },
  })
  if (!activeSession) return jsonError("Aucun poste ouvert", 409)

  const sessionOrders = await prisma.order.findMany({
    where: {
      createdAt: { gte: activeSession.openedAt },
      userId: activeSession.userId,
      status: "LIVREE",
    },
    select: {
      totalPrice: true,
      totalCost: true,
      totalProfit: true,
      companyShare: true,
      smithShare: true,
    },
  })
  const ordersCount = sessionOrders.length
  const totalSales = sessionOrders.reduce((sum, order) => sum + order.totalPrice, 0)
  const totalCost = sessionOrders.reduce((sum, order) => sum + order.totalCost, 0)
  const totalProfit = sessionOrders.reduce((sum, order) => sum + order.totalProfit, 0)
  const companyShare = sessionOrders.reduce((sum, order) => sum + order.companyShare, 0)
  const smithShare = sessionOrders.reduce((sum, order) => sum + order.smithShare, 0)
  const cashDeposit = totalCost + companyShare

  const closed = await prisma.shopSession.update({
    where: { id: activeSession.id },
    data: { closedAt: new Date(), ordersCount, totalSales, totalProfit },
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "SHOP_CLOSE",
      details: `Poste fermé après ${ordersCount} factures`,
    },
  })

  await webhookShopClose({
    username: session!.user.name ?? "Forgeron",
    openedAt: activeSession.openedAt,
    ordersCount,
    totalSales,
    totalCost,
    totalProfit,
    companyShare,
    smithShare,
    cashDeposit,
  })

  return NextResponse.json(closed)
}
