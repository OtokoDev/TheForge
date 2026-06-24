import type { Metadata } from "next"
import "./globals.css"
import { Toaster } from "@/components/ui/sonner"

export const metadata: Metadata = {
  title: "Forge RP",
  description: "Gestion de stock, commandes et boutique pour une forge RP.",
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="fr" className="dark">
      <body>
        {children}
        <Toaster richColors closeButton />
      </body>
    </html>
  )
}
