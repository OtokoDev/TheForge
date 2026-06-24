import { NextResponse } from "next/server"
import { prisma } from "@/lib/prisma"

export const dynamic = "force-dynamic"

export async function GET() {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const activeSession = await prisma.shopSession.findFirst({
    where: { closedAt: null },
    include: { user: { select: { username: true, avatar: true } } },
    orderBy: { openedAt: "desc" },
  })

  const todaySessions = await prisma.shopSession.findMany({
    where: { openedAt: { gte: today } },
    include: { user: { select: { username: true } } },
    orderBy: { openedAt: "desc" },
  })

  return NextResponse.json({
    isOpen: Boolean(activeSession),
    activeSession,
    todaySessions,
  })
}
