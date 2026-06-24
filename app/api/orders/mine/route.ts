import { NextResponse } from "next/server"
import { auth } from "@/auth"
import { prisma } from "@/lib/prisma"
import { jsonError } from "@/lib/api"

export const dynamic = "force-dynamic"

export async function GET() {
  const session = await auth()
  if (!session?.user) return jsonError("Non autorisé", 401)

  const orders = await prisma.order.findMany({
    where: { userId: session.user.id },
    include: { items: { include: { product: true } } },
    orderBy: { createdAt: "desc" },
  })

  return NextResponse.json(orders)
}
