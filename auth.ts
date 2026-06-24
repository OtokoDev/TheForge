import NextAuth from "next-auth"
import Discord from "next-auth/providers/discord"
import { prisma } from "@/lib/prisma"

export const { handlers, auth, signIn, signOut } = NextAuth({
  session: { strategy: "jwt" },
  providers: [
    Discord({
      clientId: process.env.DISCORD_CLIENT_ID,
      clientSecret: process.env.DISCORD_CLIENT_SECRET,
    }),
  ],
  callbacks: {
    async signIn({ profile }) {
      if (!profile?.id) return false

      const discordId = String(profile.id)
      const username =
        typeof profile.username === "string" ? profile.username : "Utilisateur Discord"
      const avatarHash = typeof profile.avatar === "string" ? profile.avatar : null
      const avatar = avatarHash
        ? `https://cdn.discordapp.com/avatars/${discordId}/${avatarHash}.png`
        : null

      const user = await prisma.user.upsert({
        where: { discordId },
        create: { discordId, username, avatar },
        update: { username, avatar },
      })

      return user.isActive
    },
    async jwt({ token, profile }) {
      if (profile?.id) {
        token.discordId = String(profile.id)
      }

      const discordId = token.discordId ?? token.sub
      const userId = token.id ?? token.sub
      if (discordId || userId) {
        const user = await prisma.user.findFirst({
          where: {
            OR: [
              ...(discordId ? [{ discordId: String(discordId) }] : []),
              ...(userId ? [{ id: String(userId) }] : []),
            ],
          },
          select: {
            id: true,
            discordId: true,
            username: true,
            inGameName: true,
            avatar: true,
            role: true,
            isActive: true,
          },
        })

        if (user) {
          token.id = user.id
          token.discordId = user.discordId
          token.name = user.inGameName ?? user.username
          token.discordUsername = user.username
          token.inGameName = user.inGameName ?? undefined
          token.picture = user.avatar ?? undefined
          token.role = user.role
          token.isActive = user.isActive
        }
      }

      return token
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.id = String(token.id ?? "")
        session.user.discordId = String(token.discordId ?? "")
        session.user.discordUsername = String(token.discordUsername ?? token.name ?? "")
        session.user.inGameName = typeof token.inGameName === "string" ? token.inGameName : null
        session.user.name = token.name
        session.user.image = token.picture
        session.user.role = (token.role as "ADMIN" | "GERANT" | "FORGERON") ?? "FORGERON"
        session.user.isActive = token.isActive !== false
      }

      return session
    },
  },
  pages: {
    signIn: "/",
    error: "/access-denied",
  },
})
