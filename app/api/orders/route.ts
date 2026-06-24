import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { auth } from "@/auth"
import { guardApi, jsonError } from "@/lib/api"
import { recipeCost } from "@/lib/format"
import { buildResourceConsumptions, splitProfit } from "@/lib/order-finance"
import { hasRole } from "@/lib/permissions"
import { prisma } from "@/lib/prisma"
import { webhookOrderConfirmed } from "@/lib/webhooks"

export const dynamic = "force-dynamic"

const orderSchema = z.object({
  items: z
    .array(
      z.object({
        productId: z.string().min(1),
        quantity: z.coerce.number().int().positive(),
        itemSource: z.enum(["MADE_NOW", "FROM_STOCK"]).default("MADE_NOW"),
      }),
    )
    .min(1),
  status: z
    .enum(["EN_ATTENTE", "EN_FABRICATION", "PRETE", "LIVREE", "ANNULEE"])
    .optional(),
  clientNote: z.string().max(1000).optional(),
})

class StockError extends Error {}

type OrderItemRow = {
  productId: string
  quantity: number
  unitPrice: number
  unitCost: number
  itemSource: "MADE_NOW" | "FROM_STOCK"
  stockBefore: number | null
  stockAfter: number | null
}

function stockErrorMessage(name: string, requested: number, available: number) {
  return `Stock insuffisant pour ${name} : ${requested} demandé(s), ${available} disponible(s).`
}

export async function GET() {
  const { response } = await guardApi("FORGERON")
  if (response) return response

  const orders = await prisma.order.findMany({
    include: { items: { include: { product: true } }, user: true },
    orderBy: { createdAt: "desc" },
  })

  return NextResponse.json(orders)
}

export async function POST(req: NextRequest) {
  const session = await auth()
  if (!session?.user) return jsonError("Non autorisé", 401)
  if (!session.user.isActive) return jsonError("Compte désactivé", 403)

  const parsed = orderSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Données commande invalides")

  const requestedStatus = parsed.data.status ?? "EN_ATTENTE"
  if (requestedStatus !== "EN_ATTENTE" && !hasRole(session.user.role, "FORGERON")) {
    return jsonError("Seuls les forgerons peuvent définir le statut d'une facture", 403)
  }

  const requestedItems = parsed.data.items
  const products = await prisma.product.findMany({
    where: {
      id: { in: requestedItems.map((item) => item.productId) },
      isActive: true,
    },
    include: { recipe: { include: { ingredient: true } } },
  })

  if (products.length !== new Set(requestedItems.map((item) => item.productId)).size) {
    return jsonError("Un produit est introuvable", 404)
  }

  let totalPrice = 0
  let totalCost = 0
  const orderItems: OrderItemRow[] = requestedItems.map((item) => {
    const product = products.find((candidate) => candidate.id === item.productId)!
    const unitCost = recipeCost(product.recipe)
    totalPrice += product.sellPrice * item.quantity
    totalCost += unitCost * item.quantity

    return {
      productId: product.id,
      quantity: item.quantity,
      unitPrice: product.sellPrice,
      unitCost,
      itemSource: item.itemSource,
      stockBefore: null,
      stockAfter: null,
    }
  })
  const { totalProfit, smithShare, companyShare } = splitProfit(totalPrice, totalCost)
  const consumptionRows = buildResourceConsumptions(
    requestedItems.map((item) => {
      const product = products.find((candidate) => candidate.id === item.productId)!
      return { quantity: item.quantity, itemSource: item.itemSource, product }
    }),
  )

  let order
  try {
    order = await prisma.$transaction(async (tx) => {
      if (requestedStatus === "LIVREE") {
        for (const item of orderItems) {
          if (item.itemSource !== "FROM_STOCK") continue

          const product = products.find((candidate) => candidate.id === item.productId)!
          const result = await tx.product.updateMany({
            where: { id: item.productId, finishedStock: { gte: item.quantity } },
            data: { finishedStock: { decrement: item.quantity } },
          })

          if (result.count !== 1) {
            const current = await tx.product.findUnique({
              where: { id: item.productId },
              select: { finishedStock: true },
            })
            throw new StockError(
              stockErrorMessage(product.name, item.quantity, current?.finishedStock ?? 0),
            )
          }

          const updated = await tx.product.findUniqueOrThrow({
            where: { id: item.productId },
            select: { finishedStock: true },
          })
          item.stockAfter = updated.finishedStock
          item.stockBefore = updated.finishedStock + item.quantity
        }

        for (const consumption of consumptionRows) {
          const result = await tx.ingredient.updateMany({
            where: { id: consumption.ingredientId, currentStock: { gte: consumption.quantity } },
            data: { currentStock: { decrement: consumption.quantity } },
          })

          if (result.count !== 1) {
            const ingredient = await tx.ingredient.findUnique({
              where: { id: consumption.ingredientId },
              select: { name: true, currentStock: true },
            })
            throw new StockError(
              stockErrorMessage(
                ingredient?.name ?? "une ressource",
                consumption.quantity,
                ingredient?.currentStock ?? 0,
              ),
            )
          }
        }
      }

      const created = await tx.order.create({
        data: {
          userId: session.user.id,
          status: requestedStatus,
          deliveredAt: requestedStatus === "LIVREE" ? new Date() : null,
          totalPrice,
          totalCost,
          totalProfit,
          smithShare,
          companyShare,
          clientNote: parsed.data.clientNote,
          items: { create: orderItems },
        },
        include: { items: { include: { product: true } }, user: true },
      })

      if (requestedStatus === "LIVREE") {
        for (const consumption of consumptionRows) {
          await tx.orderResourceConsumption.create({
            data: {
              orderId: created.id,
              ingredientId: consumption.ingredientId,
              quantity: consumption.quantity,
              unitCost: consumption.unitCost,
              totalCost: consumption.totalCost,
            },
          })
        }
      }

      return created
    })
  } catch (error) {
    if (error instanceof StockError) return jsonError(error.message)
    throw error
  }

  await prisma.activityLog.create({
    data: {
      userId: session.user.id,
      action: requestedStatus === "LIVREE" ? "INVOICE_CREATED" : "ORDER_CREATED",
      details:
        requestedStatus === "LIVREE"
          ? `Facture #${order.orderNumber} créée`
          : `Commande #${order.orderNumber} créée`,
    },
  })

  await webhookOrderConfirmed({
    orderNumber: order.orderNumber,
    username: order.user.username,
    items: order.items.map((item) => ({
      name: item.product.name,
      quantity: item.quantity,
    })),
    totalPrice: order.totalPrice,
    totalCost: order.totalCost,
    totalProfit: order.totalProfit,
    companyShare: order.companyShare,
    smithShare: order.smithShare,
    status: order.status,
    clientNote: order.clientNote,
  })

  return NextResponse.json(order, { status: 201 })
}
