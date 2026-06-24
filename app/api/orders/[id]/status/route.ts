import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { prisma } from "@/lib/prisma"
import { guardApi, jsonError } from "@/lib/api"
import { buildResourceConsumptions, splitProfit } from "@/lib/order-finance"

export const dynamic = "force-dynamic"

const statusSchema = z.object({
  status: z.enum(["EN_ATTENTE", "EN_FABRICATION", "PRETE", "LIVREE", "ANNULEE"]),
  internalNote: z.string().optional(),
})

class StockError extends Error {}

function stockErrorMessage(name: string, requested: number, available: number) {
  return `Stock insuffisant pour ${name} : ${requested} demandé(s), ${available} disponible(s).`
}

export async function PUT(req: NextRequest, { params }: { params: { id: string } }) {
  const { response, session } = await guardApi("FORGERON")
  if (response) return response

  const parsed = statusSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Statut invalide")

  let order
  try {
    order = await prisma.$transaction(async (tx) => {
      const current = await tx.order.findUnique({
        where: { id: params.id },
        include: {
          consumptions: true,
          items: {
            include: { product: { include: { recipe: { include: { ingredient: true } } } } },
          },
        },
      })
      if (!current) throw new Error("Commande introuvable")

      if (parsed.data.status === "LIVREE" && current.status !== "LIVREE") {
        for (const item of current.items) {
          if (item.itemSource !== "FROM_STOCK") continue

          const result = await tx.product.updateMany({
            where: { id: item.productId, finishedStock: { gte: item.quantity } },
            data: { finishedStock: { decrement: item.quantity } },
          })

          if (result.count !== 1) {
            const product = await tx.product.findUnique({
              where: { id: item.productId },
              select: { name: true, finishedStock: true },
            })
            throw new StockError(
              stockErrorMessage(
                product?.name ?? "un produit",
                item.quantity,
                product?.finishedStock ?? 0,
              ),
            )
          }

          const updated = await tx.product.findUniqueOrThrow({
            where: { id: item.productId },
            select: { finishedStock: true },
          })
          await tx.orderItem.update({
            where: { id: item.id },
            data: {
              stockBefore: updated.finishedStock + item.quantity,
              stockAfter: updated.finishedStock,
            },
          })
        }

        if (current.consumptions.length === 0) {
          const consumptions = buildResourceConsumptions(current.items)
          for (const consumption of consumptions) {
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

            await tx.orderResourceConsumption.create({
              data: {
                orderId: current.id,
                ingredientId: consumption.ingredientId,
                quantity: consumption.quantity,
                unitCost: consumption.unitCost,
                totalCost: consumption.totalCost,
              },
            })
          }
        }
      }
      const finance = splitProfit(current.totalPrice, current.totalCost)

      return tx.order.update({
        where: { id: params.id },
        data: {
          status: parsed.data.status,
          internalNote: parsed.data.internalNote,
          deliveredAt: parsed.data.status === "LIVREE" ? new Date() : current.deliveredAt,
          ...finance,
        },
        include: { items: { include: { product: true } }, user: true },
      })
    })
  } catch (error) {
    if (error instanceof StockError) return jsonError(error.message)
    throw error
  }

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "ORDER_STATUS_UPDATED",
      details: `Commande #${order.orderNumber} -> ${order.status}`,
    },
  })

  return NextResponse.json(order)
}
