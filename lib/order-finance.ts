type RecipeLine = {
  quantity: number
  ingredient: {
    id: string
    unitCost: number
  }
}

type OrderItemInput = {
  quantity: number
  itemSource?: "MADE_NOW" | "FROM_STOCK"
  product: {
    recipe: RecipeLine[]
  }
}

export function splitProfit(totalPrice: number, totalCost: number) {
  const totalProfit = totalPrice - totalCost
  const smithShare = totalProfit * 0.5
  const companyShare = totalProfit * 0.5

  return { totalProfit, smithShare, companyShare }
}

export function buildResourceConsumptions(items: OrderItemInput[]) {
  const consumptions = new Map<
    string,
    { ingredientId: string; quantity: number; unitCost: number; totalCost: number }
  >()

  for (const item of items) {
    if (item.itemSource === "FROM_STOCK") continue

    for (const recipe of item.product.recipe) {
      const quantity = recipe.quantity * item.quantity
      const current =
        consumptions.get(recipe.ingredient.id) ?? {
          ingredientId: recipe.ingredient.id,
          quantity: 0,
          unitCost: recipe.ingredient.unitCost,
          totalCost: 0,
        }

      current.quantity += quantity
      current.totalCost += quantity * recipe.ingredient.unitCost
      consumptions.set(recipe.ingredient.id, current)
    }
  }

  return Array.from(consumptions.values())
}
