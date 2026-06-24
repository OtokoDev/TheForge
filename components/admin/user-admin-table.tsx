"use client"

import { useRouter } from "next/navigation"
import { useTransition } from "react"
import { toast } from "sonner"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Button } from "@/components/ui/button"

type UserRow = {
  id: string
  role: "ADMIN" | "GERANT" | "FORGERON"
  isActive: boolean
}

export function UserRoleSelect({ user }: { user: UserRow }) {
  const router = useRouter()
  const [isPending, startTransition] = useTransition()

  function update(payload: Partial<UserRow>) {
    startTransition(async () => {
      const response = await fetch(`/api/admin/users/${user.id}/role`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      })

      if (!response.ok) {
        const error = await response.json().catch(() => null)
        toast.error(error?.error ?? "Mise à jour impossible")
        return
      }

      toast.success("Utilisateur mis à jour")
      router.refresh()
    })
  }

  return (
    <div className="flex items-center gap-2">
      <Select value={user.role} onValueChange={(role) => update({ role: role as UserRow["role"] })}>
        <SelectTrigger className="w-36">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="FORGERON">FORGERON</SelectItem>
          <SelectItem value="GERANT">GERANT</SelectItem>
          <SelectItem value="ADMIN">ADMIN</SelectItem>
        </SelectContent>
      </Select>
      <Button
        variant={user.isActive ? "outline" : "default"}
        size="sm"
        disabled={isPending}
        onClick={() => update({ isActive: !user.isActive })}
      >
        {user.isActive ? "Désactiver" : "Activer"}
      </Button>
    </div>
  )
}
