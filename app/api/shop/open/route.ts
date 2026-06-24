import { NextResponse } from "next/server"
import { prisma } from "@/lib/prisma"
import { guardApi, jsonError } from "@/lib/api"
import { webhookShopOpen } from "@/lib/webhooks"

export const dynamic = "force-dynamic"

export async function POST() {
  const { response, session } = await guardApi("FORGERON")
  if (response) return response

  const existing = await prisma.shopSession.findFirst({ where: { closedAt: null } })
  if (existing) return jsonError("Un poste est déjà ouvert", 409)

  const shopSession = await prisma.shopSession.create({
    data: { userId: session!.user.id },
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "SHOP_OPEN",
      details: "Poste ouvert",
    },
  })

  await webhookShopOpen({ username: session!.user.name ?? "Forgeron" })

  return NextResponse.json(shopSession, { status: 201 })
}
