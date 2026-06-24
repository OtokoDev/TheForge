import { NextRequest, NextResponse } from "next/server"
import { getToken } from "next-auth/jwt"

const PUBLIC_ROUTES = ["/access-denied", "/api/auth"]
const FORGERON_ROUTES = [
  "/dashboard",
  "/facturation",
  "/statistiques",
  "/rachat",
  "/stock",
  "/commandes",
  "/catalogue",
  "/profil",
  "/panier",
  "/mes-commandes",
]
const ADMIN_ROUTES = ["/admin"]

export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl

  if (pathname === "/" || PUBLIC_ROUTES.some((route) => pathname.startsWith(route))) {
    return NextResponse.next()
  }

  const token = await getToken({
    req,
    secret: process.env.AUTH_SECRET ?? process.env.NEXTAUTH_SECRET,
  })

  if (!token || token.isActive === false) {
    return NextResponse.redirect(new URL("/", req.url))
  }

  const role = token.role
  if (
    ADMIN_ROUTES.some((route) => pathname.startsWith(route)) &&
    role !== "ADMIN" &&
    role !== "GERANT"
  ) {
    return NextResponse.redirect(new URL("/access-denied", req.url))
  }

  if (
    FORGERON_ROUTES.some((route) => pathname.startsWith(route)) &&
    role !== "ADMIN" &&
    role !== "GERANT" &&
    role !== "FORGERON"
  ) {
    return NextResponse.redirect(new URL("/access-denied", req.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
}
