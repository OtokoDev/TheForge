"use client"

import { FormEvent, useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { Save } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export function ProfileForm({ initialName }: { initialName: string }) {
  const router = useRouter()
  const [inGameName, setInGameName] = useState(initialName)
  const [isPending, startTransition] = useTransition()

  function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    startTransition(async () => {
      const response = await fetch("/api/profile", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ inGameName }),
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        toast.error(payload?.error ?? "Profil impossible à modifier")
        return
      }

      toast.success("Profil mis à jour")
      router.refresh()
    })
  }

  return (
    <form className="flex flex-col gap-3" onSubmit={saveProfile}>
      <Label htmlFor="inGameName">Pseudo IG</Label>
      <div className="flex gap-2">
        <Input
          id="inGameName"
          value={inGameName}
          onChange={(event) => setInGameName(event.target.value)}
          placeholder="Ton pseudo en jeu"
        />
        <Button disabled={isPending}>
          <Save data-icon="inline-start" />
          Sauver
        </Button>
      </div>
    </form>
  )
}
