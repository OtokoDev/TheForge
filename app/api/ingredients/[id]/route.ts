import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { prisma } from "@/lib/prisma"
import { guardApi, jsonError } from "@/lib/api"

export const dynamic = "force-dynamic"

const ingredientSchema = z.object({
  unitCost: z.coerce.number().nonnegative().optional(),
  alertThreshold: z.coerce.number().int().nonnegative().optional(),
})

export async function PUT(req: NextRequest, { params }: { params: { id: string } }) {
  const { response, session } = await guardApi("GERANT")
  if (response) return response

  const parsed = ingredientSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Données ingrédient invalides")
  if (parsed.data.unitCost === undefined && parsed.data.alertThreshold === undefined) {
    return jsonError("Aucun champ modifiable fourni")
  }

  const ingredient = await prisma.ingredient.update({
    where: { id: params.id },
    data: parsed.data,
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "INGREDIENT_UPDATED",
      details: `Matière ${ingredient.name} modifiée`,
    },
  })

  return NextResponse.json(ingredient)
}

export async function DELETE(_: NextRequest, { params }: { params: { id: string } }) {
  const { response, session } = await guardApi("ADMIN")
  if (response) return response

  const ingredient = await prisma.ingredient.update({
    where: { id: params.id },
    data: { isActive: false },
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "INGREDIENT_DELETED",
      details: `Matière ${ingredient.name} archivée`,
    },
  })

  return NextResponse.json(ingredient)
}
