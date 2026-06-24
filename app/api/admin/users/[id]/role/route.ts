import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { prisma } from "@/lib/prisma"
import { guardApi, jsonError } from "@/lib/api"

export const dynamic = "force-dynamic"

const schema = z.object({
  role: z.enum(["ADMIN", "GERANT", "FORGERON"]).optional(),
  isActive: z.boolean().optional(),
})

export async function PUT(req: NextRequest, { params }: { params: { id: string } }) {
  const { response, session } = await guardApi("ADMIN")
  if (response) return response

  const parsed = schema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Données utilisateur invalides")

  const user = await prisma.user.update({
    where: { id: params.id },
    data: parsed.data,
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "USER_UPDATED",
      details: `${user.username} -> ${user.role}, actif=${user.isActive}`,
    },
  })

  return NextResponse.json(user)
}
