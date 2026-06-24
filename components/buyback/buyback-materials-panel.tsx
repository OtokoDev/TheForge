"use client"

import { useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { Save } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatMoney } from "@/lib/format"

type BuybackMaterial = {
  id: string
  name: string
  unit: string
  unitCost: number
  buybackDetail: string | null
  currentStock: number
}

type Draft = {
  unitCost: string
  buybackDetail: string
}

export function BuybackMaterialsPanel({
  materials,
  canEdit,
}: {
  materials: BuybackMaterial[]
  canEdit: boolean
}) {
  const router = useRouter()
  const [isPending, startTransition] = useTransition()
  const [drafts, setDrafts] = useState<Record<string, Draft>>(() =>
    Object.fromEntries(
      materials.map((material) => [
        material.id,
        {
          unitCost: String(material.unitCost),
          buybackDetail: material.buybackDetail ?? "",
        },
      ]),
    ),
  )

  function updateDraft(id: string, patch: Partial<Draft>) {
    setDrafts((current) => ({
      ...current,
      [id]: {
        ...current[id],
        ...patch,
      },
    }))
  }

  function saveMaterial(material: BuybackMaterial) {
    const draft = drafts[material.id]

    startTransition(async () => {
      const response = await fetch(`/api/buyback-materials/${material.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          unitCost: draft.unitCost,
          buybackDetail: draft.buybackDetail,
        }),
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        toast.error(payload?.error ?? "Modification impossible")
        return
      }

      toast.success(`Prix rachat ${material.name} modifié`)
      router.refresh()
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Matières premières rachetées</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Matière</TableHead>
              <TableHead>Prix rachat</TableHead>
              <TableHead>Détail</TableHead>
              <TableHead>Stock</TableHead>
              {canEdit ? <TableHead className="w-12" /> : null}
            </TableRow>
          </TableHeader>
          <TableBody>
            {materials.map((material) => {
              const draft = drafts[material.id]

              return (
                <TableRow key={material.id}>
                  <TableCell>
                    <p className="font-medium">{material.name}</p>
                    <p className="text-xs text-muted-foreground">{material.unit}</p>
                  </TableCell>
                  <TableCell className="min-w-36">
                    {canEdit ? (
                      <Input
                        type="number"
                        min="0"
                        step="0.01"
                        value={draft.unitCost}
                        onChange={(event) =>
                          updateDraft(material.id, { unitCost: event.target.value })
                        }
                      />
                    ) : (
                      formatMoney(material.unitCost)
                    )}
                  </TableCell>
                  <TableCell className="min-w-56">
                    {canEdit ? (
                      <Input
                        value={draft.buybackDetail}
                        onChange={(event) =>
                          updateDraft(material.id, { buybackDetail: event.target.value })
                        }
                        placeholder="Ex : 1 or les 3"
                      />
                    ) : (
                      (material.buybackDetail ?? "-")
                    )}
                  </TableCell>
                  <TableCell>{material.currentStock}</TableCell>
                  {canEdit ? (
                    <TableCell>
                      <Button
                        size="icon"
                        variant="outline"
                        disabled={isPending}
                        onClick={() => saveMaterial(material)}
                      >
                        <Save data-icon="inline-start" />
                      </Button>
                    </TableCell>
                  ) : null}
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  )
}
