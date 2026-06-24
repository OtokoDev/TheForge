import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { prisma } from "@/lib/prisma"
import { guardApi, jsonError } from "@/lib/api"

export const dynamic = "force-dynamic"

const movementSchema = z.object({
  ingredientId: z.string().min(1),
  type: z.enum(["IN", "OUT"]),
  quantity: z.coerce.number().int().positive(),
  reason: z.string().optional(),
})

export async function POST(req: NextRequest) {
  const { response, session } = await guardApi("FORGERON")
  if (response) return response

  const parsed = movementSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Mouvement de stock invalide")

  const movement = await prisma.$transaction(async (tx) => {
    const ingredient = await tx.ingredient.findFirst({
      where: { id: parsed.data.ingredientId, isActive: true },
    })
    if (!ingredient) throw new Error("Matière introuvable")

    const nextStock =
      parsed.data.type === "IN"
        ? ingredient.currentStock + parsed.data.quantity
        : ingredient.currentStock - parsed.data.quantity
    if (nextStock < 0) throw new Error("Stock insuffisant")

    await tx.ingredient.update({
      where: { id: ingredient.id },
      data: { currentStock: nextStock },
    })

    return tx.stockMovement.create({
      data: {
        ...parsed.data,
        userId: session!.user.id,
      },
      include: { ingredient: true, user: true },
    })
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "STOCK_MOVEMENT",
      details: `${movement.type} ${movement.quantity} ${movement.ingredient.name}`,
    },
  })

  return NextResponse.json(movement, { status: 201 })
}
