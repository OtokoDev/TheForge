import { NextResponse } from "next/server"
import { prisma } from "@/lib/prisma"
import { guardApi } from "@/lib/api"

export const dynamic = "force-dynamic"

export async function GET() {
  const { response } = await guardApi("ADMIN")
  if (response) return response

  const users = await prisma.user.findMany({
    orderBy: { createdAt: "desc" },
    include: { _count: { select: { orders: true, activityLogs: true } } },
  })

  return NextResponse.json(users)
}
