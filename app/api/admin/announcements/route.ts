import { NextRequest, NextResponse } from "next/server"
import { z } from "zod"
import { guardApi, jsonError } from "@/lib/api"
import { prisma } from "@/lib/prisma"

export const dynamic = "force-dynamic"

const announcementSchema = z.object({
  title: z.string().trim().min(2).max(80),
  message: z.string().trim().min(2).max(500),
  color: z.enum(["GREEN", "YELLOW", "RED"]),
})

export async function POST(req: NextRequest) {
  const { response, session } = await guardApi("ADMIN")
  if (response) return response

  const parsed = announcementSchema.safeParse(await req.json())
  if (!parsed.success) return jsonError("Annonce invalide")

  const announcement = await prisma.announcement.create({
    data: {
      ...parsed.data,
      authorId: session!.user.id,
    },
  })

  await prisma.activityLog.create({
    data: {
      userId: session!.user.id,
      action: "ANNOUNCEMENT_CREATED",
      details: `Annonce ${announcement.title} créée`,
    },
  })

  return NextResponse.json(announcement, { status: 201 })
}
