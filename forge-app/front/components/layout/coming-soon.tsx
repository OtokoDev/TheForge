import { Hammer } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"

/** Placeholder pour les modules dont l'API back n'existe pas encore (migration par lots). */
export function ComingSoon({ title }: { title: string }) {
  return (
    <Card>
      <CardContent className="flex flex-col items-center gap-3 py-16 text-center">
        <Hammer className="size-8 text-muted-foreground" />
        <h2 className="text-lg font-semibold">{title}</h2>
        <p className="max-w-md text-sm text-muted-foreground">
          Module en cours de migration vers la nouvelle architecture (back Micronaut).
          Disponible dans un prochain lot.
        </p>
      </CardContent>
    </Card>
  )
}
