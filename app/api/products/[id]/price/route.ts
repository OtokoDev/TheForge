import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { guardApi, jsonError } from "@/lib/api"
import { prisma } from "@/lib/prisma"

export const dynamic = "force-dynamic"

const productPriceSchema = z.object({
  sellPrice: z.coerce.number().nonnegative(),
})

export async function PUT(req: NextRequest, { params }: { params: { id: string } }) {
  const { response, session } = await guardApi("ADMIN")
  if (response) return response

  const parsed = productPriceSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Prix de vente invalide")

  const product = await prisma.product.update({
    where: { id: params.id },
    data: { sellPrice: parsed.data.sellPrice },
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "PRODUCT_PRICE_UPDATED",
      details: `Prix de vente ${product.name} modifié`,
    },
  })

  return NextResponse.json(product)
}
