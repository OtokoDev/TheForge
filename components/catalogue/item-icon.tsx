import {
  Axe,
  BowArrow,
  Gem,
  Hammer,
  HardHat,
  Hand,
  HandHelping,
  Package,
  Shield,
  Shirt,
  SportShoe,
  Sword,
} from "lucide-react"

export type ProductItemType =
  | "DAGGER"
  | "SWORD"
  | "AXE"
  | "MACE"
  | "BOW"
  | "ARMOR"
  | "BOOTS"
  | "GLOVES"
  | "HELMET"
  | "SHIELD"
  | "RESOURCE"
  | "OTHER"

export type ProductHandRequired = "ONE" | "TWO" | null

const itemTypeIcons = {
  DAGGER: Sword,
  SWORD: Sword,
  AXE: Axe,
  MACE: Hammer,
  BOW: BowArrow,
  ARMOR: Shirt,
  BOOTS: SportShoe,
  GLOVES: HandHelping,
  HELMET: HardHat,
  SHIELD: Shield,
  RESOURCE: Gem,
  OTHER: Package,
} satisfies Record<ProductItemType, typeof Package>

const itemTypeLabels = {
  DAGGER: "Dague",
  SWORD: "Épée",
  AXE: "Hache",
  MACE: "Masse",
  BOW: "Arc",
  ARMOR: "Armure",
  BOOTS: "Bottes",
  GLOVES: "Gants",
  HELMET: "Casque",
  SHIELD: "Bouclier",
  RESOURCE: "Ressource",
  OTHER: "Autre",
} satisfies Record<ProductItemType, string>

export function ItemIcon({
  itemType,
  handRequired,
}: {
  itemType: ProductItemType
  handRequired: ProductHandRequired
}) {
  const Icon = itemTypeIcons[itemType]

  return (
    <div className="flex shrink-0 items-center gap-1.5 text-muted-foreground">
      <span
        className="flex size-8 items-center justify-center rounded-md border bg-muted/40 text-foreground"
        title={itemTypeLabels[itemType]}
      >
        <Icon data-icon="inline-start" />
      </span>
      {handRequired ? (
        <span
          className="flex h-8 min-w-8 items-center justify-center gap-1 rounded-md border bg-muted/40 px-2 text-xs font-medium text-foreground"
          title={handRequired === "ONE" ? "1 main" : "2 mains"}
        >
          <Hand data-icon="inline-start" />
          {handRequired === "ONE" ? "1" : "2"}
        </span>
      ) : null}
    </div>
  )
}
