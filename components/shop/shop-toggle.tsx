"use client"

import { useRouter } from "next/navigation"
import { useTransition } from "react"
import { DoorClosed, DoorOpen } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"

export function ShopToggle({ isOpen }: { isOpen: boolean }) {
  const router = useRouter()
  const [isPending, startTransition] = useTransition()

  function toggleShop() {
    startTransition(async () => {
      const response = await fetch(isOpen ? "/api/shop/close" : "/api/shop/open", {
        method: "POST",
      })
      const payload = await response.json().catch(() => null)

      if (!response.ok) {
        toast.error(payload?.error ?? "Action impossible")
        return
      }

      toast.success(isOpen ? "Poste fermé" : "Poste ouvert")
      router.refresh()
    })
  }

  return (
    <Button
      size="lg"
      variant={isOpen ? "destructive" : "default"}
      disabled={isPending}
      onClick={toggleShop}
      className="w-full sm:w-auto"
    >
      {isOpen ? <DoorClosed data-icon="inline-start" /> : <DoorOpen data-icon="inline-start" />}
      {isOpen ? "Terminer le poste" : "Prendre le poste"}
    </Button>
  )
}
