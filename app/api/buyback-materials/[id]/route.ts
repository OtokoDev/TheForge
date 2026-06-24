import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { guardApi, jsonError } from "@/lib/api"
import { prisma } from "@/lib/prisma"

export const dynamic = "force-dynamic"

const buybackMaterialSchema = z.object({
  unitCost: z.coerce.number().nonnegative(),
  buybackDetail: z.string().max(160).optional().nullable(),
})

export async function PUT(req: NextRequest, { params }: { params: { id: string } }) {
  const { response, session } = await guardApi("ADMIN")
  if (response) return response

  const parsed = buybackMaterialSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Données rachat invalides")

  const material = await prisma.ingredient.update({
    where: { id: params.id, materialType: "RAW" },
    data: {
      unitCost: parsed.data.unitCost,
      buybackDetail: parsed.data.buybackDetail?.trim() || null,
    },
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "BUYBACK_PRICE_UPDATED",
      details: `Prix rachat ${material.name} modifié`,
    },
  })

  return NextResponse.json(material)
}
