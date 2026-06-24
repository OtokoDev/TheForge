import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { guardApi, jsonError } from "@/lib/api"
import { prisma } from "@/lib/prisma"

export const dynamic = "force-dynamic"

const recipeSchema = z.object({
  ingredientId: z.string().min(1),
  quantity: z.coerce.number().int().positive(),
})

const productSchema = z.object({
  name: z.string().min(2),
  description: z.string().optional().nullable(),
  category: z.string().min(2),
  categoryId: z.string().optional().nullable(),
  imageUrl: z.string().url().optional().or(z.literal("")).nullable(),
  sellPrice: z.coerce.number().nonnegative(),
  itemType: z
    .enum(["DAGGER", "SWORD", "AXE", "MACE", "BOW", "ARMOR", "BOOTS", "GLOVES", "HELMET", "SHIELD", "RESOURCE", "OTHER"])
    .optional(),
  handRequired: z.enum(["ONE", "TWO"]).optional().nullable(),
  finishedStock: z.coerce.number().int().nonnegative(),
  isActive: z.boolean().optional(),
  recipe: z.array(recipeSchema).optional(),
})

export async function PUT(req: NextRequest, { params }: { params: { id: string } }) {
  const { response, session } = await guardApi("FORGERON")
  if (response) return response

  const parsed = productSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Données produit invalides")

  const { recipe, ...data } = parsed.data
  const product = await prisma.$transaction(async (tx) => {
    if (recipe) {
      await tx.recipe.deleteMany({ where: { productId: params.id } })
    }

    return tx.product.update({
      where: { id: params.id },
      data: {
        ...data,
        imageUrl: data.imageUrl || null,
        categoryId: data.categoryId || null,
        recipe: recipe
          ? {
              create: recipe.map((item) => ({
                ingredientId: item.ingredientId,
                quantity: item.quantity,
              })),
            }
          : undefined,
      },
      include: { categoryRef: true, recipe: { include: { ingredient: true } } },
    })
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "PRODUCT_UPDATED",
      details: `Produit ${product.name} modifié`,
    },
  })

  return NextResponse.json(product)
}

export async function DELETE(_: NextRequest, { params }: { params: { id: string } }) {
  const { response, session } = await guardApi("ADMIN")
  if (response) return response

  const product = await prisma.product.update({
    where: { id: params.id },
    data: { isActive: false },
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "PRODUCT_ARCHIVED",
      details: `Produit ${product.name} archivé`,
    },
  })

  return NextResponse.json(product)
}
