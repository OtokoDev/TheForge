import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { guardApi, jsonError } from "@/lib/api"
import { prisma } from "@/lib/prisma"

export const dynamic = "force-dynamic"

const categorySchema = z.object({
  name: z.string().min(2).max(40),
})

export async function GET() {
  const categories = await prisma.category.findMany({
    orderBy: { name: "asc" },
    include: { _count: { select: { products: true } } },
  })

  return NextResponse.json(categories)
}

export async function POST(req: NextRequest) {
  const { response, session } = await guardApi("FORGERON")
  if (response) return response

  const parsed = categorySchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Nom de catégorie invalide")

  const category = await prisma.category.upsert({
    where: { name: parsed.data.name.trim() },
    update: {},
    create: { name: parsed.data.name.trim() },
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "CATEGORY_CREATED",
      details: `Catégorie ${category.name} créée`,
    },
  })

  return NextResponse.json(category, { status: 201 })
}
