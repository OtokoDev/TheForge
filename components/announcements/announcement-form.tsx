"use client"

import { FormEvent, useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { Megaphone } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"

export function AnnouncementForm() {
  const router = useRouter()
  const [color, setColor] = useState<"GREEN" | "YELLOW" | "RED">("GREEN")
  const [isPending, startTransition] = useTransition()

  function createAnnouncement(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)

    startTransition(async () => {
      const response = await fetch("/api/admin/announcements", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: String(form.get("title") ?? ""),
          message: String(form.get("message") ?? ""),
          color,
        }),
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        toast.error(payload?.error ?? "Annonce impossible à créer")
        return
      }

      toast.success("Annonce publiée")
      formElement.reset()
      setColor("GREEN")
      router.refresh()
    })
  }

  return (
    <form className="grid gap-3" onSubmit={createAnnouncement}>
      <Label htmlFor="title">Titre</Label>
      <Input id="title" name="title" required />
      <Label htmlFor="message">Message</Label>
      <Textarea id="message" name="message" required />
      <Label>Couleur</Label>
      <Select value={color} onValueChange={(value) => setColor((value ?? "GREEN") as typeof color)}>
        <SelectTrigger>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="GREEN">Vert</SelectItem>
          <SelectItem value="YELLOW">Jaune</SelectItem>
          <SelectItem value="RED">Rouge</SelectItem>
        </SelectContent>
      </Select>
      <Button type="submit" disabled={isPending}>
        <Megaphone data-icon="inline-start" />
        Publier
      </Button>
    </form>
  )
}
