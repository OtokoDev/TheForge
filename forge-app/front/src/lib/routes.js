import Dashboard from '../pages/Dashboard.svelte'
import Stub from '../pages/Stub.svelte'

// Table de routes svelte-spa-router (hash). Dashboard porté ; le reste = stub (P2).
export const routes = {
  '/': Dashboard,
  '/dashboard': Dashboard,
  '/facturation': Stub,
  '/commandes': Stub,
  '/stock': Stub,
  '/rachat': Stub,
  '/main-courante': Stub,
  '/statistiques': Stub,
  '/catalogue': Stub,
  '/configuration': Stub,
  '/staff': Stub,
  '/systeme': Stub,
  '/admin': Stub,
  '/profil': Stub,
  '/access-denied': Stub,
  '*': Stub,
}
