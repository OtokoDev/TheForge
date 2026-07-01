// Écrans masquables par compagnie (piloté par SYSTEM). key = clé de route (href).
// Dashboard, Configuration et les écrans SYSTEM ne sont pas masquables.
export const SCREENS = [
  { key: '/facturation', label: 'Facturation' },
  { key: '/commandes', label: 'Commandes' },
  { key: '/atelier', label: 'Atelier' },
  { key: '/stock', label: 'Stock' },
  { key: '/approvisionnement', label: 'Approvisionnement' },
  { key: '/commerce', label: 'Commerce' },
  { key: '/statistiques', label: 'Finance' },
  { key: '/carte', label: 'Carte' },
]

// Routes alias → écran canonique (pour que la garde masque aussi les deep-links historiques).
export const ROUTE_ALIASES = {
  '/achats': '/approvisionnement',
  '/rachat': '/approvisionnement',
  '/finance': '/statistiques',
}
