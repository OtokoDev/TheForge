import Dashboard from '../pages/Dashboard.svelte'
import Facturation from '../pages/Facturation.svelte'
import Commandes from '../pages/Commandes.svelte'
import Atelier from '../pages/Atelier.svelte'
import Stock from '../pages/Stock.svelte'
import Rachat from '../pages/Rachat.svelte'
import MainCourante from '../pages/MainCourante.svelte'
import Statistiques from '../pages/Statistiques.svelte'
import Catalogue from '../pages/Catalogue.svelte'
import Configuration from '../pages/Configuration.svelte'
import Staff from '../pages/Staff.svelte'
import Systeme from '../pages/Systeme.svelte'
import Profil from '../pages/Profil.svelte'
import AccessDenied from '../pages/AccessDenied.svelte'

// Table de routes svelte-spa-router (hash).
export const routes = {
  '/': Dashboard,
  '/dashboard': Dashboard,
  '/facturation': Facturation,
  '/commandes': Commandes,
  '/atelier': Atelier,
  '/stock': Stock,
  '/rachat': Rachat,
  '/main-courante': MainCourante,
  '/statistiques': Statistiques,
  '/catalogue': Catalogue,
  '/configuration': Configuration,
  '/staff': Staff,
  '/systeme': Systeme,
  '/admin': Systeme,
  '/profil': Profil,
  '/access-denied': AccessDenied,
  '*': Dashboard,
}
