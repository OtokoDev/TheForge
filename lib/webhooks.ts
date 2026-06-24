import { formatDuration, formatMoney, formatOrderNumber } from "@/lib/format"
import { prisma } from "@/lib/prisma"

type WebhookType = "ORDER_CONFIRMED" | "SHOP_OPEN" | "SHOP_CLOSE"

async function sendToDiscord(url: string | undefined, payload: object, type: WebhookType) {
  const payloadStr = JSON.stringify(payload)
  let success = false
  let error: string | undefined

  if (!url || url.includes("ORDERS_WEBHOOK") || url.includes("SHOP_WEBHOOK")) {
    error = "Webhook non configuré"
  } else {
    for (let attempt = 1; attempt <= 3; attempt++) {
      try {
        const response = await fetch(url, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: payloadStr,
        })

        if (response.ok) {
          success = true
          error = undefined
          break
        }

        error = `HTTP ${response.status}`
      } catch (cause) {
        error = cause instanceof Error ? cause.message : String(cause)
      }

      if (!success && attempt < 3) {
        await new Promise((resolve) => setTimeout(resolve, attempt * 1000))
      }
    }
  }

  await prisma.webhookLog.create({
    data: { type, payload: payloadStr, success, error },
  })

  return success
}

export async function webhookOrderConfirmed(order: {
  orderNumber: number
  username: string
  items: { name: string; quantity: number }[]
  totalPrice: number
  totalCost: number
  totalProfit: number
  companyShare: number
  smithShare: number
  status?: string
  clientNote?: string | null
}) {
  const fields: object[] = [
    { name: "Forgeron", value: order.username, inline: true },
    { name: "N° facture", value: formatOrderNumber(order.orderNumber), inline: true },
    {
      name: "Articles",
      value: order.items.map((item) => `${item.quantity}x ${item.name}`).join("\n") || "—",
      inline: false,
    },
    { name: "Prix total", value: formatMoney(order.totalPrice), inline: true },
    { name: "Prix de craft", value: formatMoney(order.totalCost), inline: true },
    { name: "Bénéfice total", value: formatMoney(order.totalProfit), inline: true },
    { name: "Part forge", value: formatMoney(order.companyShare), inline: true },
    { name: "Part forgeron", value: formatMoney(order.smithShare), inline: true },
    { name: "Statut", value: order.status ?? "LIVREE", inline: true },
  ]

  if (order.clientNote) {
    fields.push({ name: "Note", value: order.clientNote, inline: false })
  }

  return sendToDiscord(
    process.env.DISCORD_WEBHOOK_ORDERS,
    {
      embeds: [
        {
          title: "Nouvelle facture",
          color: 5763719,
          fields,
          footer: { text: "Forge RP • Facture enregistrée" },
          timestamp: new Date().toISOString(),
        },
      ],
    },
    "ORDER_CONFIRMED",
  )
}

export async function webhookShopOpen(forgeron: { username: string }) {
  return sendToDiscord(
    process.env.DISCORD_WEBHOOK_SHOP,
    {
      embeds: [
        {
          title: "La boutique est ouverte",
          color: 5763719,
          fields: [
            { name: "Forgeron", value: forgeron.username, inline: true },
            {
              name: "Heure d'ouverture",
              value: new Date().toLocaleTimeString("fr-FR", {
                hour: "2-digit",
                minute: "2-digit",
              }),
              inline: true,
            },
          ],
          footer: { text: "Forge RP" },
          timestamp: new Date().toISOString(),
        },
      ],
    },
    "SHOP_OPEN",
  )
}

export async function webhookShopClose(data: {
  username: string
  openedAt: Date
  ordersCount: number
  totalSales: number
  totalCost: number
  totalProfit: number
  companyShare: number
  smithShare: number
  cashDeposit: number
}) {
  return sendToDiscord(
    process.env.DISCORD_WEBHOOK_SHOP,
    {
      embeds: [
        {
          title: "La boutique est fermée",
          color: 15548997,
          fields: [
            { name: "Forgeron", value: data.username, inline: true },
            {
              name: "Heure de fermeture",
              value: new Date().toLocaleTimeString("fr-FR", {
                hour: "2-digit",
                minute: "2-digit",
              }),
              inline: true,
            },
            { name: "Durée de session", value: formatDuration(data.openedAt), inline: true },
            { name: "Factures traitées", value: String(data.ordersCount), inline: true },
            { name: "CA réalisé", value: formatMoney(data.totalSales), inline: true },
            { name: "Prix de craft", value: formatMoney(data.totalCost), inline: true },
            { name: "Bénéfice total", value: formatMoney(data.totalProfit), inline: true },
            { name: "Bénéfice forge", value: formatMoney(data.companyShare), inline: true },
            { name: "Bénéfice forgeron", value: formatMoney(data.smithShare), inline: true },
            { name: "À déposer au coffre", value: formatMoney(data.cashDeposit), inline: true },
          ],
          footer: { text: "Forge RP" },
          timestamp: new Date().toISOString(),
        },
      ],
    },
    "SHOP_CLOSE",
  )
}
