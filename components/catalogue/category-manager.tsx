"use client"

import { FormEvent, useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { Plus } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

export function CategoryManager() {
  const router = useRouter()
  const [name, setName] = useState("")
  const [isPending, startTransition] = useTransition()

  function createCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextName = name.trim()
    if (!nextName) return

    startTransition(async () => {
      const response = await fetch("/api/categories", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: nextName }),
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        toast.error(payload?.error ?? "Création impossible")
        return
      }

      setName("")
      toast.success("Catégorie créée")
      router.refresh()
    })
  }

  return (
    <form className="flex flex-col gap-2 sm:flex-row" onSubmit={createCategory}>
      <Input
        value={name}
        onChange={(event) => setName(event.target.value)}
        placeholder="Fer, Acier, Cuir, Outils..."
        className="sm:w-72"
      />
      <Button type="submit" disabled={isPending || !name.trim()}>
        <Plus data-icon="inline-start" />
        Créer une catégorie
      </Button>
    </form>
  )
}
