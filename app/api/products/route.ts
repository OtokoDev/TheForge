import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { prisma } from "@/lib/prisma"
import { guardApi, jsonError } from "@/lib/api"

export const dynamic = "force-dynamic"

const recipeSchema = z.object({
  ingredientId: z.string().min(1),
  quantity: z.coerce.number().int().positive(),
})

const productSchema = z.object({
  name: z.string().min(2),
  description: z.string().optional().nullable(),
  category: z.string().min(2).default("Divers"),
  categoryId: z.string().optional().nullable(),
  imageUrl: z.string().url().optional().or(z.literal("")).nullable(),
  sellPrice: z.coerce.number().nonnegative(),
  itemType: z
    .enum(["DAGGER", "SWORD", "AXE", "MACE", "BOW", "ARMOR", "BOOTS", "GLOVES", "HELMET", "SHIELD", "RESOURCE", "OTHER"])
    .optional(),
  handRequired: z.enum(["ONE", "TWO"]).optional().nullable(),
  finishedStock: z.coerce.number().int().nonnegative().default(0),
  recipe: z.array(recipeSchema).default([]),
})

export async function GET() {
  const products = await prisma.product.findMany({
    where: { isActive: true },
    include: { categoryRef: true, recipe: { include: { ingredient: true } } },
    orderBy: { createdAt: "desc" },
  })

  return NextResponse.json(products)
}

export async function POST(req: NextRequest) {
  const { response, session } = await guardApi("FORGERON")
  if (response) return response

  const parsed = productSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Données produit invalides")

  const { recipe, ...data } = parsed.data
  const product = await prisma.product.create({
    data: {
      ...data,
      imageUrl: data.imageUrl || null,
      categoryId: data.categoryId || null,
      recipe: {
        create: recipe.map((item) => ({
          ingredientId: item.ingredientId,
          quantity: item.quantity,
        })),
      },
    },
    include: { categoryRef: true, recipe: { include: { ingredient: true } } },
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "PRODUCT_CREATED",
      details: `Produit ${product.name} créé`,
    },
  })

  return NextResponse.json(product, { status: 201 })
}
