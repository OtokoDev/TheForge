import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { prisma } from "@/lib/prisma"
import { guardApi, jsonError } from "@/lib/api"

export const dynamic = "force-dynamic"

const productMovementSchema = z.object({
  productId: z.string().min(1),
  type: z.enum(["IN", "OUT"]),
  quantity: z.coerce.number().int().positive(),
})

export async function POST(req: NextRequest) {
  const { response, session } = await guardApi("FORGERON")
  if (response) return response

  const parsed = productMovementSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Mouvement de produit fini invalide")

  const { productId, quantity, type } = parsed.data

  try {
    const product = await prisma.$transaction(async (tx) => {
      const existing = await tx.product.findFirst({
        where: { id: productId, isActive: true },
        select: { id: true, name: true, finishedStock: true },
      })
      if (!existing) throw new Error("Produit introuvable")

      if (type === "OUT") {
        const result = await tx.product.updateMany({
          where: { id: existing.id, isActive: true, finishedStock: { gte: quantity } },
          data: { finishedStock: { decrement: quantity } },
        })
        if (result.count === 0) throw new Error("Stock produit insuffisant")
      } else {
        await tx.product.update({
          where: { id: existing.id },
          data: { finishedStock: { increment: quantity } },
        })
      }

      return tx.product.findUniqueOrThrow({
        where: { id: existing.id },
        select: { id: true, name: true, finishedStock: true },
      })
    })

    await prisma.activityLog.create({
      data: {
        userId: session!.user.id,
        action: "PRODUCT_STOCK_MOVEMENT",
        details: `${type} ${quantity} ${product.name} (stock fini ${product.finishedStock})`,
      },
    })

    return NextResponse.json(product, { status: 201 })
  } catch (error) {
    return jsonError(error instanceof Error ? error.message : "Mouvement produit impossible")
  }
}
