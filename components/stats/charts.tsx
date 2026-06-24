import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { formatMoney } from "@/lib/format"

export type ChartPoint = {
  label: string
  value: number
}

export function LineChartCard({
  title,
  points,
  money = false,
}: {
  title: string
  points: ChartPoint[]
  money?: boolean
}) {
  const max = Math.max(...points.map((point) => point.value), 1)
  const width = 560
  const height = 180
  const path = points
    .map((point, index) => {
      const x = points.length === 1 ? width / 2 : (index / (points.length - 1)) * width
      const y = height - (point.value / max) * height
      return `${index === 0 ? "M" : "L"} ${x.toFixed(1)} ${y.toFixed(1)}`
    })
    .join(" ")

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <svg viewBox={`0 0 ${width} ${height}`} className="h-44 w-full overflow-visible">
          <path d={path} fill="none" stroke="currentColor" strokeWidth="3" className="text-primary" />
          {points.map((point, index) => {
            const x = points.length === 1 ? width / 2 : (index / (points.length - 1)) * width
            const y = height - (point.value / max) * height
            return <circle key={`${point.label}-${index}`} cx={x} cy={y} r="4" className="fill-primary" />
          })}
        </svg>
        <div className="mt-2 flex justify-between text-xs text-muted-foreground">
          <span>{points[0]?.label ?? "-"}</span>
          <span>{points[points.length - 1]?.label ?? "-"}</span>
        </div>
        <p className="mt-2 text-sm text-muted-foreground">
          Pic : {money ? formatMoney(max) : max.toLocaleString("fr-FR")}
        </p>
      </CardContent>
    </Card>
  )
}

export function BarChartCard({
  title,
  points,
  money = false,
}: {
  title: string
  points: ChartPoint[]
  money?: boolean
}) {
  const max = Math.max(...points.map((point) => point.value), 1)

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {points.length === 0 ? (
          <p className="text-sm text-muted-foreground">Aucune donnée.</p>
        ) : (
          points.slice(0, 8).map((point) => (
            <div key={point.label} className="grid gap-1">
              <div className="flex justify-between gap-3 text-sm">
                <span className="truncate">{point.label}</span>
                <span className="shrink-0 text-muted-foreground">
                  {money ? formatMoney(point.value) : point.value.toLocaleString("fr-FR")}
                </span>
              </div>
              <div className="h-2 rounded-full bg-muted">
                <div
                  className="h-2 rounded-full bg-primary"
                  style={{ width: `${Math.max(4, (point.value / max) * 100)}%` }}
                />
              </div>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  )
}
