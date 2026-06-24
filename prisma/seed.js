const { PrismaClient } = require("@prisma/client")

const prisma = new PrismaClient()

const rawMaterials = [
  ["seed-minerai-fer", "Minerai de fer", "minerai", 0.33, "1 or les 3", 50, 10],
  ["seed-corindon-brut", "Corindon brut", "pièce", 0.33, "1 or les 3", 0, 5],
  ["seed-charbon", "Charbon pauvre", "pièce", 0.5, "1 or les 2", 8, 15],
  ["seed-charbon-basique", "Charbon basique", "pièce", 1, "1 or pièce", 0, 5],
  ["seed-charbon-briquette", "Charbon briquette", "pièce", 1, "2 or pièce", 0, 5],
  ["seed-charbon-coke", "Charbon coke", "pièce", 2, "3 or pièce", 0, 5],
  ["seed-cuir", "Cuir", "pièce", 1, "1 or pièce", 30, 5],
  ["seed-bande-cuir", "Bande de cuir", "bande", 0.25, "4 par cuir", 0, 10],
  ["seed-petit-bois", "Petit bois", "pièce", 0.1, "1 or les 10", 0, 10],
  ["seed-pierre-lune-brute", "Pierre de lune brute", "pièce", 10, "10 or pièce", 0, 2],
  ["seed-vif-argent", "Minerai de vif-argent", "minerai", 10, "10 or pièce", 0, 2],
  [
    "seed-orichalque",
    "Minerai d'orichalque",
    "minerai",
    8,
    "8 or pièce - futur équipement orque",
    0,
    2,
  ],
  ["seed-or", "Minerai d'or", "minerai", 4, "4 or pièce", 0, 2],
  ["seed-argent", "Minerai d'argent", "minerai", 4, "4 or pièce", 0, 2],
  ["seed-malachite", "Malachite brute", "pièce", 15, "15 or pièce", 0, 2],
  [
    "seed-dwemer",
    "Lingot dwemer",
    "lingot",
    250,
    "250 or pièce - fonte de ferraille dwemer",
    0,
    1,
  ],
]

const transformedMaterials = [
  ["seed-lingot-fer", "Lingot de fer", "lingot", 0.53, 0, 5],
  ["seed-lingot-acier", "Lingot d'acier", "lingot", 1.66, 0, 5],
  ["seed-corindon-raffine", "Corindon raffiné", "pièce", 1.06, 0, 5],
  ["seed-pierre-lune-raffinee", "Pierre de lune raffinée", "pièce", 22, 0, 2],
  ["seed-lingot-vif-argent", "Lingot de vif-argent", "lingot", 24, 0, 2],
  ["seed-lingot-or", "Lingot d'or", "lingot", 9, 0, 2],
  ["seed-lingot-argent", "Lingot d'argent", "lingot", 9, 0, 2],
  ["seed-lingot-malachite", "Lingot de malachite", "lingot", 34, 0, 2],
]

const craftCatalog = {
  Armures: {
    "Stuff cuirs": {
      Casque: { leathers: 2, "bande de cuirs": 1 },
      Armure: { leathers: 4, "bande de cuirs": 3 },
      "Armure Boiled": { leathers: 4, "bande de cuirs": 3 },
      Gants: { leathers: 1, "bande de cuirs": 2 },
      Bottes: { leathers: 2, "bande de cuirs": 2 },
    },
    "Stuff Acier": {
      Casque: { "bande de cuirs": 2, "lingots de cuirs": 2, "lingot de fer": 1 },
      Armure: { "bande de cuirs": 3, "lingots de cuirs": 4, "lingot de fer": 1 },
      Gants: { "bande de cuirs": 2, "lingots acier": 2, "lingots de fer": 1 },
      Bottes: { "bande de cuirs": 2, "lingot de Fer": 1, "lingot acier": 3 },
    },
    "Stuff fer": {
      Casque: { "bande de cuirs": 2, "lingot de fer": 1 },
      Armure: { "bande de cuirs": 3, "lingot de fer": 5 },
      "Armure à bande": { "bande de cuirs": 3, "lingot de fer": 5, "corindon raffiné": 1 },
      Gants: { "bande de cuirs": 2, "lingots de fer": 2 },
      Bottes: { "bande de cuirs": 2, "lingot de Fer": 3 },
    },
  },
  Armes: {
    "Armes Fer": {
      "Arc long": { "petits bois": 8, "bande de cuirs": 2, leather: 1 },
      Dague: { "lingot de fer": 1, "bande de cuir": 1 },
      "Épée 1 main": { "lingot de fer": 2, "bande de cuir": 1 },
      "Hache 1 main": { "lingot de fer": 2, "bande de cuir": 2 },
      "Masse 1 main": { "lingot de fer": 3, "bande de cuir": 2 },
      "Épée 2 mains": { "lingot de fer": 4, "bande de cuir": 2 },
      "Marteau 2 mains": { "lingot de fer": 4, "bande de cuir": 3 },
      "Hache 2 mains": { "lingot de fer": 4, "bande de cuir": 2 },
    },
    "Arme Acier": {
      "Arc de chasse": {
        "petits bois": 8,
        "bande de cuirs": 2,
        leather: 1,
        "lingot acier": 2,
      },
      Dague: { "lingot de fer": 1, "bande de cuir": 1, "lingot acier": 1 },
      "Épée 1 main": { "lingot acier": 2, "bande de cuir": 1, "lingot fer": 1 },
      "Hache 1 main": { "lingot acier": 2, "bande de cuir": 2, "lingot fer": 1 },
      "Masse 1 main": { "lingot de fer": 3, "bande de cuir": 2 },
      "Épée 2 mains": { "lingot de fer": 2, "bande de cuir": 3, "lingot acier": 4 },
      "Marteau 2 mains": { "lingot de fer": 1, "bande de cuir": 3, "lingot acier": 4 },
      "Hache 2 mains": { "lingot de fer": 1, "bande de cuir": 2, "lingot acier": 4 },
      "Flèche en acier": { "petit Bois": 1, "lingot acier": 1 },
    },
  },
}

const materialAlias = {
  leather: "Cuir",
  leathers: "Cuir",
  cuir: "Cuir",
  cuirs: "Cuir",
  "lingot de cuir": "Cuir",
  "lingots de cuir": "Cuir",
  "lingot de cuirs": "Cuir",
  "lingots de cuirs": "Cuir",
  "bande de cuir": "Bande de cuir",
  "bande de cuirs": "Bande de cuir",
  "bandes de cuir": "Bande de cuir",
  "bandes de cuirs": "Bande de cuir",
  "petit bois": "Petit bois",
  "petits bois": "Petit bois",
  "lingot de fer": "Lingot de fer",
  "lingots de fer": "Lingot de fer",
  "lingot fer": "Lingot de fer",
  "lingots fer": "Lingot de fer",
  "lingot acier": "Lingot d'acier",
  "lingots acier": "Lingot d'acier",
  "lingot d'acier": "Lingot d'acier",
  "lingots d'acier": "Lingot d'acier",
  "corindon raffiné": "Corindon raffiné",
}

function normalizeMaterialName(name) {
  return materialAlias[name.trim().toLocaleLowerCase("fr-FR").replace(/\s+/g, " ")] ?? name.trim()
}

function slugify(value) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "")
}

function getItemMetadata(groupName, productName) {
  const name = productName.toLocaleLowerCase("fr-FR")
  const group = groupName.toLocaleLowerCase("fr-FR")

  if (name.includes("dague")) return { itemType: "DAGGER", handRequired: "ONE" }
  if (name.includes("épée") || name.includes("epee")) {
    return { itemType: "SWORD", handRequired: name.includes("2 main") ? "TWO" : "ONE" }
  }
  if (name.includes("hache")) {
    return { itemType: "AXE", handRequired: name.includes("2 main") ? "TWO" : "ONE" }
  }
  if (name.includes("marteau")) return { itemType: "MACE", handRequired: "TWO" }
  if (name.includes("masse")) return { itemType: "MACE", handRequired: "ONE" }
  if (name.includes("arc")) return { itemType: "BOW", handRequired: "TWO" }
  if (name.includes("flèche") || name.includes("fleche")) return { itemType: "RESOURCE", handRequired: null }
  if (name.includes("casque")) return { itemType: "HELMET", handRequired: null }
  if (name.includes("gants")) return { itemType: "GLOVES", handRequired: null }
  if (name.includes("bottes")) return { itemType: "BOOTS", handRequired: null }
  if (name.includes("armure") || group.includes("stuff")) return { itemType: "ARMOR", handRequired: null }

  return { itemType: "OTHER", handRequired: null }
}

async function main() {
  const admin = await prisma.user.upsert({
    where: { discordId: "dev-admin" },
    update: {},
    create: {
      discordId: "dev-admin",
      username: "Admin Forge",
      inGameName: "Admin Forge",
      role: "ADMIN",
      isActive: true,
    },
  })

  const categoryNames = [
    "Fer",
    "Acier",
    "Cuir",
    "Outils",
    ...Object.values(craftCatalog).flatMap((family) => Object.keys(family)),
  ]
  const categories = []
  for (const name of [...new Set(categoryNames)]) {
    categories.push(
      await prisma.category.upsert({
        where: { name },
        update: {},
        create: { name },
      }),
    )
  }
  const categoryByName = Object.fromEntries(categories.map((category) => [category.name, category]))

  const ingredients = []
  for (const [id, name, unit, unitCost, buybackDetail, currentStock, alertThreshold] of rawMaterials) {
    ingredients.push(
      await prisma.ingredient.upsert({
        where: { id },
        update: {
          name,
          unit,
          materialType: "RAW",
          alertThreshold,
        },
        create: {
          id,
          name,
          unit,
          unitCost,
          materialType: "RAW",
          buybackDetail,
          currentStock,
          alertThreshold,
        },
      }),
    )
  }

  for (const [id, name, unit, unitCost, currentStock, alertThreshold] of transformedMaterials) {
    ingredients.push(
      await prisma.ingredient.upsert({
        where: { id },
        update: {
          name,
          unit,
          unitCost,
          materialType: "TRANSFORMED",
          alertThreshold,
        },
        create: {
          id,
          name,
          unit,
          unitCost,
          materialType: "TRANSFORMED",
          buybackDetail: null,
          currentStock,
          alertThreshold,
        },
      }),
    )
  }

  const ingredientByName = Object.fromEntries(
    ingredients.map((ingredient) => [ingredient.name, ingredient]),
  )
  const activeSeedProductIds = []

  for (const [familyName, groups] of Object.entries(craftCatalog)) {
    for (const [groupName, products] of Object.entries(groups)) {
      for (const [productName, recipeInput] of Object.entries(products)) {
        const recipe = Object.entries(recipeInput).map(([inputName, quantity]) => {
          const materialName = normalizeMaterialName(inputName)
          const ingredient = ingredientByName[materialName]
          if (!ingredient) {
            throw new Error(`Matière introuvable pour ${productName}: ${inputName}`)
          }

          return { ingredientId: ingredient.id, quantity }
        })
        const sellPrice = recipe.reduce((total, item) => {
          const ingredient = ingredients.find((entry) => entry.id === item.ingredientId)
          return total + item.quantity * ingredient.unitCost
        }, 0)
        const id = `seed-craft-${slugify(familyName)}-${slugify(groupName)}-${slugify(productName)}`
        const metadata = getItemMetadata(groupName, productName)
        activeSeedProductIds.push(id)

        await prisma.product.upsert({
          where: { id },
          update: {
            name: productName,
            description: `${familyName} - ${groupName}`,
            category: groupName,
            categoryId: categoryByName[groupName].id,
            itemType: metadata.itemType,
            handRequired: metadata.handRequired,
            isActive: true,
            recipe: {
              deleteMany: {},
              create: recipe,
            },
          },
          create: {
            id,
            name: productName,
            description: `${familyName} - ${groupName}`,
            category: groupName,
            categoryId: categoryByName[groupName].id,
            sellPrice,
            itemType: metadata.itemType,
            handRequired: metadata.handRequired,
            finishedStock: 0,
            recipe: { create: recipe },
          },
        })
      }
    }
  }

  await prisma.product.updateMany({
    where: {
      id: {
        in: ["seed-epee-courte", "seed-epee-longue", "seed-casque-fer", "seed-plastron-cuir"],
      },
    },
    data: { isActive: false },
  })

  await prisma.activityLog.create({
    data: {
      userId: admin.id,
      action: "SEED",
      details: `${activeSeedProductIds.length} crafts catalogue Forge RP synchronisés`,
    },
  })
}

main()
  .then(async () => {
    await prisma.$disconnect()
  })
  .catch(async (error) => {
    console.error(error)
    await prisma.$disconnect()
    process.exit(1)
  })
