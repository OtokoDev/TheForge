import { NextRequest, NextResponse } from "next/server"
import { prisma } from "@/lib/prisma"
import { guardApi } from "@/lib/api"

export const dynamic = "force-dynamic"

export async function GET(req: NextRequest) {
  const { response } = await guardApi("ADMIN")
  if (response) return response

  const page = Number(req.nextUrl.searchParams.get("page") ?? 1)
  const take = 50

  const [items, total] = await Promise.all([
    prisma.webhookLog.findMany({
      orderBy: { createdAt: "desc" },
      skip: (Math.max(page, 1) - 1) * take,
      take,
    }),
    prisma.webhookLog.count(),
  ])

  return NextResponse.json({ items, total, page, take })
}
