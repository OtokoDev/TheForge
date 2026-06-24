import type { DefaultSession } from "next-auth"

declare module "next-auth" {
  interface Session {
    user: {
      id: string
      discordId: string
      discordUsername: string
      inGameName: string | null
      role: "ADMIN" | "GERANT" | "FORGERON"
      isActive: boolean
    } & DefaultSession["user"]
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    id?: string
    discordId?: string
    discordUsername?: string
    inGameName?: string
    role?: "ADMIN" | "GERANT" | "FORGERON"
    isActive?: boolean
  }
}
