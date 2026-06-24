import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { guardApi, jsonError } from "@/lib/api"
import { prisma } from "@/lib/prisma"

export const dynamic = "force-dynamic"

const profileSchema = z.object({
  inGameName: z.string().trim().min(2).max(60),
})

export async function PUT(req: NextRequest) {
  const { response, session } = await guardApi("FORGERON")
  if (response) return response

  const parsed = profileSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Pseudo IG invalide")

  const user = await prisma.user.update({
    where: { id: session!.user.id },
    data: { inGameName: parsed.data.inGameName },
  })

  await prisma.activityLog.create({
    data: {
      userId: user.id,
      action: "PROFILE_UPDATED",
      details: `Pseudo IG défini sur ${user.inGameName}`,
    },
  })

  return NextResponse.json(user)
}
