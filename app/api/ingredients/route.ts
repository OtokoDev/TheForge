import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { prisma } from "@/lib/prisma"
import { guardApi, jsonError } from "@/lib/api"

export const dynamic = "force-dynamic"

const ingredientSchema = z.object({
  name: z.string().min(2),
  unit: z.string().min(1).default("unité"),
  unitCost: z.coerce.number().nonnegative().default(0),
  currentStock: z.coerce.number().int().default(0),
  alertThreshold: z.coerce.number().int().nonnegative().default(5),
})

export async function GET() {
  const { response } = await guardApi("FORGERON")
  if (response) return response

  const ingredients = await prisma.ingredient.findMany({
    where: { isActive: true },
    orderBy: { name: "asc" },
  })

  return NextResponse.json(ingredients)
}

export async function POST(req: NextRequest) {
  const { response, session } = await guardApi("ADMIN")
  if (response) return response
  if (session!.user.role !== "ADMIN") return jsonError("Accès refusé", 403)

  const parsed = ingredientSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Données ingrédient invalides")

  const ingredient = await prisma.ingredient.create({ data: parsed.data })
  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "INGREDIENT_CREATED",
      details: `Matière ${ingredient.name} créée`,
    },
  })

  return NextResponse.json(ingredient, { status: 201 })
}
