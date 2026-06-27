# Cahier des charges — TheForge v2

> **Statut :** brouillon v0.1 — document de travail à fournir à Claude Code
> **Date :** 2026-06-25
> **Auteur :** Bryan (reprise/industrialisation du POC de la forge RP Skyrim)

---

## 1. Contexte et objectifs

Le projet est un outil de gestion pour des activités économiques d'un serveur Skyrim RP. Un POC existe (Next.js + Prisma + Postgres + NextAuth/Discord + shadcn/Tailwind). Il valide les fonctionnalités forge (catalogue, recettes, ventes, prises de poste, webhooks Discord, split de bénéfice). Il sert de **spécification fonctionnelle de référence** et de **charte graphique**, mais le back est repris de zéro.

**Objectif principal :** un suivi solide et traçable du **stock**, des **comptes** et des **mouvements**, avec facturation (qui décrémente les ressources et incrémente l'argent), configuration des objets/valeurs/recettes, et ouverture à plusieurs « business » (forge, compagnie, et autres types à venir).

**Objectifs de qualité :**

- Traçabilité totale des mouvements de stock et d'argent (jamais de perte d'historique).
- Cohérence monétaire stricte (aucun flottant sur les montants effectifs).
- Sécurité centralisée côté back.
- Industrialisation : migrations versionnées, CI/CD, déploiement VPS.

---

## 2. Principes de conception (directives projet)

Ces principes (issus du `CLAUDE.md` du dépôt) encadrent toute la réalisation :

- **Simplicité d'abord.** Aucune feature, abstraction ou configurabilité non demandée. Le code minimum qui résout le besoin.
- **Pas d'hypothèse cachée.** En cas d'ambiguïté, on nomme le flou et on demande avant d'implémenter.
- **Changements chirurgicaux.** On ne refactore pas ce qui marche, on respecte le style existant.
- **Exécution pilotée par les buts.** Chaque lot a un critère de succès vérifiable (cf. §15).

---

## 3. Architecture cible

### 3.1 Vue d'ensemble

- **Front :** application Next.js existante **conservée**, transformée en **pur client** de l'API. NextAuth est retiré (l'auth passe côté back). La charte graphique (shadcn/Tailwind) est inchangée.
- **Back :** nouvelle application **Micronaut (Java)**, **monolithe modulaire**, propriétaire du domaine et du schéma de base. Centralise la sécurité.
- **Base :** **PostgreSQL neuve** (pas de reprise des données du POC). Migrations gérées par **Flyway**.
- **Auth :** **OAuth2 Discord géré par Micronaut** (flow authorization-code), émission d'une session/JWT validée à chaque requête. Discord ne sert qu'à l'authentification et aux notifications.
- **Notifications :** **webhooks Discord** sortants, déclenchés par événements de domaine.
- **Déploiement :** VPS. La **CI/CD (GitHub Actions)** est prévue **bien plus tard** (cf. §12) ; déploiement manuel au départ.

### 3.2 Découpage modulaire (monolithe)

Un seul déploiement, découpé en modules métier à frontières nettes :

- `iam` — identité, OAuth Discord, rôles & permissions.
- `business` — les business (tenants), appartenance, sélecteur de business courant.
- `catalog` — items globaux et recettes.
- `valuation` — valeurs des items par business (historisées).
- `ledger` — comptes/coffres et journal de mouvements (stock + argent).
- `forge` — sessions (prises de poste), factures, taxe, récap.
- `treasury` — créances/dettes envers les farmeurs, paiements.
- `commerce` — transferts/factures inter-business.
- `stats` — agrégats et historiques.
- `notifications` — webhooks Discord.
- `audit` — journal d'activité.

> **Pas de microservices.** L'échelle ne le justifie pas ; le monolithe modulaire suffit et reste simple à exploiter.

### 3.3 Intégration front/back

- Le navigateur ↔ Micronaut directement (API REST/JSON). Gestion **CORS** côté Micronaut.
- Le front initie le login en redirigeant vers l'endpoint OAuth de Micronaut ; le token de session est ensuite porté sur chaque appel API.
- Le contrat d'API est défini côté back (OpenAPI souhaité) et consommé par le front.

### 3.4 Organisation du code back

- **Découpage primaire par domaine fonctionnel** : les modules du §3.2 sont les unités d'organisation du code (un package/dossier racine par domaine). Ils matérialisent les **bounded contexts** DDD.
- **Découpage technique secondaire** à l'intérieur de chaque domaine, en couches :
  - `datamodel` — le **modèle de domaine** (entités, value objects, agrégats), sans dépendance à l'infrastructure.
  - `datarepository` — la **persistance** (repositories, accès PostgreSQL).
  - `backend` — les **services applicatifs / cas d'usage** et les **contrôleurs REST**.
- **API : REST comme norme** — ressources, verbes HTTP et codes de statut standards.
- **Bonnes pratiques imposées :**
  - **DDD** — agrégats et invariants portés par le domaine, frontières de contexte = les modules, langage ubiquitaire partagé avec ce CDC.
  - **Clean code** — responsabilités claires, nommage explicite, fonctions courtes, **dépendances dirigées vers le domaine** (`datamodel` ne dépend de personne ; `datarepository` et `backend` dépendent du `datamodel`, pas l'inverse).

---

## 4. Modèle de domaine

### 4.1 Concepts clés

- **Business** — un tenant, porteur d'un **type** (`FORGE`, `COMPAGNIE`, … extensible). Une forge peut exister **sans** compagnie. Un **sélecteur de business courant** (liste déroulante en haut de l'appli) cadre tous les écrans : interface commune, données cloisonnées par business.
- **Item** — entité **globale**, commune à tous les business (minerai, lingot, épée…). Le **septime** (monnaie) est un item comme un autre, de valeur 1 : il n'existe **que des objets**, l'argent inclus. Les quantités sont toujours entières.
- **Recette** — graphe **global** : un item est composé d'autres items (craft multi-niveaux). Doit rester un **DAG** (cf. garde-fou anti-cycle §6.4).
- **Valeur (valuation)** — prix d'un item, **propre à chaque business** et **historisé** (courbe de prix dans le temps). Le septime vaut 1 partout.
- **Compte** — emplacement qui détient des items. Un **coffre** est un compte ; le stock matières de la forge est un compte. **Plusieurs comptes/coffres par business.**
- **Mouvement** — écriture immuable du **journal** (append-only), en logique partie double. Le stock d'un compte et le solde d'argent sont des **projections** (sommes de mouvements), jamais des compteurs mutables.
- **Facture** — commande qui, **à la validation**, génère atomiquement les mouvements (sortie de marchandise + entrée de septimes).
- **Session** — prise de poste d'un travailleur (forge) : ouverture, fermeture, récap.
- **Créance (dette farmeur)** — valeur des dépôts attribués à une personne, soldée par des paiements échelonnés (non rythmés).
- **Taxe** — taux par business (historisé) qui découpe le bénéfice entre la part business (« forge ») et la part travailleur.

### 4.2 Entités logiques (à matérialiser via Flyway)

> Indicatif — Claude Code détaille le DDL. **Règle transversale :** quantités = `INTEGER` ; montants d'argent effectifs (septimes) = `INTEGER` ; **prix unitaire d'item = `NUMERIC`** (décimal autorisé, ex. 0.1). Les tables `movement`, `valuation`, et les écritures de créance sont **append-only**.

- **business** : `id`, `nom`, `type`, `created_at`.
- **user** : `id`, `discord_id` (unique), `username`, `in_game_name?`, `avatar?`, `global_role` (`SYSTEM` | `STAFF` | `NONE`), `is_active`, `created_at`.
- **membership** : `id`, `user_id`, `business_id`, `role` (`ADMIN` | `MEMBRE`), `created_at` — unique `(user_id, business_id)`.
- **item** *(global)* : `id`, `name`, `type`, `hand_required?`, `is_active`. Le septime est un item seedé, non supprimable.
- **recipe_component** *(global)* : `output_item_id`, `component_item_id`, `quantity` — unique `(output_item_id, component_item_id)`. Contrainte logique : pas de cycle.
- **valuation** *(par business, historisée)* : `id`, `business_id`, `item_id`, `unit_price` (`NUMERIC`), `valid_from`, `valid_to?` (`NULL` = valeur courante). Une seule valeur courante par `(business_id, item_id)`.
- **account** : `id`, `business_id`, `name`, `kind` (`COFFRE` | `STOCK` | …), `created_at`.
- **movement** *(append-only)* : `id`, `business_id`, `item_id`, `quantity`, `from_account_id?`, `to_account_id?`, `type` (`PRODUCTION` | `CONSUMPTION` | `SALE` | `PURCHASE` | `DEPOSIT` | `WITHDRAWAL` | `TRANSFER`), `reference_type/reference_id` (facture, session, créance, commerce…), `user_id`, `created_at`. Création (`from` nul) / destruction (`to` nul) / transfert (les deux).
- **facture** : `id`, `business_id`, `numero` (séquence), `session_id?`, `status` (`BROUILLON` | `VALIDEE`), `total_amount` (`INTEGER`), `total_cost`, `total_profit`, `business_share`, `worker_share`, `tax_rate_snapshot`, `client_note?`, `internal_note?`, `created_by`, `created_at`, `validated_at?`.
- **facture_line** : `id`, `facture_id`, `item_id`, `quantity`, `unit_price_snapshot` (`NUMERIC`, modifiable avant validation), `unit_cost_snapshot`, `line_total`.
- **session** : `id`, `business_id`, `user_id`, `opened_at`, `closed_at?`, snapshots de récap, `tax_rate_snapshot`.
- **tax_rate** *(par business, historisé)* : `id`, `business_id`, `rate`, `valid_from`, `valid_to?`.
- **creance_entry** *(append-only)* : `id`, `business_id`, `farmer_user_id`, `type` (`CREDIT` | `PAIEMENT`), `amount` (`INTEGER`), `reference?`, `created_by`, `created_at`. Reste dû = Σ`CREDIT` − Σ`PAIEMENT`.
- **webhook_log** : `id`, `type`, `payload`, `success`, `error?`, `created_at`.
- **activity_log** : `id`, `user_id`, `business_id?`, `action`, `details?`, `created_at`.

---

## 5. Authentification et autorisation

### 5.1 Authentification

- OAuth2 Discord **côté Micronaut**. À la première connexion, création/maj du `user` (discord_id, username, avatar).
- Session/JWT émise par le back, validée sur chaque requête API. Gestion CORS pour le front.

### 5.2 Rôles

Rôles **fixes**, **non configurables** : pas de création de rôle, pas d'édition des permissions d'un rôle. Les permissions sont **codées en dur** par action ; on affecte seulement un rôle à un user.

- **SYSTEM** *(global, « big boss »)* — tout, partout. Gère le catalogue global (items/recettes), crée les business, configure les seeds, gère les users et les rôles globaux.
- **STAFF** *(global)* — **lecture seule** sur le stock et les factures de **tous** les business. Aucune écriture.
- **ADMIN** *(par business)* — configuration de **son** business (coffres, valeurs, taux, membres) + lecture/écriture complète sur ce business.
- **MEMBRE** *(par business)* — opérations de base dans ses business : factures, sessions, mouvements/dépôts de base, consultation. Pas de configuration.

### 5.3 Matrice de permissions (indicative, à figer dans le code)

| Action | SYSTEM | STAFF | ADMIN (son business) | MEMBRE (ses business) |
|---|---|---|---|---|
| Gérer catalogue global (items, recettes) | ✅ | ❌ | ❌ | ❌ |
| Créer un business / configurer seeds | ✅ | ❌ | ❌ | ❌ |
| Gérer users & rôles globaux | ✅ | ❌ | ❌ | ❌ |
| Config business (coffres, valeurs, taux, membres) | ✅ | ❌ | ✅ | ❌ |
| Lire stock & factures (tous business) | ✅ | ✅ | — | ❌ |
| Lire stock & factures (business courant) | ✅ | ✅ | ✅ | ✅ |
| Créer/valider une facture | ✅ | ❌ | ✅ | ✅ |
| Ouvrir/fermer une session | ✅ | ❌ | ✅ | ✅ |
| Saisir dépôts / mouvements de base | ✅ | ❌ | ✅ | ✅ |
| Enregistrer un paiement de créance | ✅ | ❌ | ✅ | ✅ (à confirmer) |

---

## 6. Règles métier

### 6.1 Argent et stock unifiés

L'argent est l'item « septime » (valeur 1). Stock et trésorerie partagent **le même journal de mouvements**. Le stock d'un compte et le solde en septimes d'un coffre sont des **projections** (Σ entrées − Σ sorties). **Stock négatif interdit** : vérifié au moment où un mouvement le provoquerait (notamment à la validation d'une facture).

### 6.2 Valeurs et historisation

- La valeur d'un item est propre au business et **historisée** : chaque changement crée une nouvelle version (la précédente est clôturée). Permet les **stats de prix dans le temps**.
- Le taux de taxe est également historisé par business.

### 6.3 Facture

- Cycle de vie : **`BROUILLON` → `VALIDEE`**. *(Pas d'annulation en v1 — cf. §14.)*
- Le prix de chaque ligne est **pré-calculé** depuis la valeur courante de l'item, puis **modifiable avant validation** (souplesse de négociation / prix spéciaux).
- À la validation, les prix sont **figés (snapshot)** sur la facture et **désolidarisés de la valeur catalogue** : un changement de valeur ultérieur ne touche aucune facture passée.
- **Arrondi par excès** des montants effectifs en septimes (le prix unitaire peut être décimal, le montant payé est un entier).
- À la validation, génération **atomique** (transaction) des mouvements : sortie de la marchandise vendue, entrée des septimes dans le coffre cible.
- Bénéfice = total facturé − coût de revient. La taxe (taux figé) découpe le bénéfice : **part business** / **part travailleur**.

### 6.4 Recettes et coût de revient

- Recette = graphe d'items. **Garde-fou anti-cycle** : interdire qu'un item entre, directement ou transitivement, dans sa propre recette (le graphe doit rester un **DAG**). Validé à la création/édition d'une recette.
- **Coût de revient récursif**, calculé **par business** : somme des valeurs (locales) des composants, en descendant la recette.

### 6.5 Session (prise de poste)

- Ouverture par un travailleur → **webhook prise de service**.
- Les factures de la session y sont rattachées.
- Fermeture → **récap** : CA, coût, bénéfice, part business, part travailleur, montant à déposer au coffre → **webhook fin de service avec récap**.

### 6.6 Dette envers les farmeurs (compagnie)

- Un dépôt d'items attribué à une personne génère une **créance** valorisée (valeur des items déposés).
- Paiements **échelonnés et non rythmés** : on enregistre des paiements ad hoc. **Reste dû = total crédité − total payé** (« j'ai payé X sur les Y »).

### 6.7 Commerce inter-business

- Transfert d'items entre comptes de **deux business différents**, via facture de commerce. Items communs → la correspondance est triviale. Contrepartie en septimes possible (achat/vente entre business).

---

## 7. Multi-business et sélecteur

- Un user peut appartenir à **plusieurs** business (ex. forgeron dans l'un, gérant dans l'autre).
- Le **business courant** se choisit via une liste déroulante en haut de l'appli ; tous les écrans (stock, factures, coffres, stats…) sont cadrés dessus.
- STAFF accède en lecture à l'ensemble des business ; les autres voient leurs business d'appartenance (+ portée globale pour SYSTEM).

---

## 8. Notifications Discord (webhooks)

Trois événements déclenchent un webhook sortant :

1. **Prise de service** (ouverture de session).
2. **Facture validée** (récap de la facture).
3. **Fin de service** (récap de session : CA, bénéfice, parts, à déposer).

- Envoi **asynchrone** sur événement de domaine (ne bloque pas la transaction métier).
- **Retry** (le POC fait 3 tentatives) + **log** de chaque envoi (`webhook_log`).
- URL des webhooks configurables (par business le cas échéant).

---

## 9. Statistiques

Indicateurs attendus, par business (et global en lecture pour STAFF) :

- **Chiffre d'affaires** (sur période).
- **Bénéfice**.
- **Valeur du stock** (Σ quantité × valeur courante).
- **Marchandise entrée** (entrées de stock / dépôts).
- **Créance restante** (dette en cours envers les farmeurs).
- **Historique des prix** et **historique du taux** de taxe.

Les stats s'appuient sur le journal de mouvements et les tables historisées (valeurs, taux).

---

## 10. Seed et configuration initiale

- Le **catalogue global** (items + recettes), septime inclus, est seedé et géré par le rôle **SYSTEM**.
- À la **création d'un business**, un seed par défaut initialise ses valeurs (valuations) et éventuellement ses coffres. Ce seed est **configurable par SYSTEM**.

---

## 11. Base de données et migrations

- **PostgreSQL neuve**, aucune reprise des données du POC (rupture assumée pour gagner du temps).
- **Flyway** pour les migrations versionnées dès le départ.
- Le `schema.prisma` du POC sert de **base de traduction** du modèle (mais Prisma n'est pas conservé).

---

## 12. Déploiement et CI/CD

- Déploiement sur **VPS** (cible : même stack que *nexis*).
- **CI/CD (GitHub Actions) reportée à bien plus tard.** Au départ, déploiement **manuel** ; aucune automatisation de pipeline n'est attendue dans les premiers lots.
- Gestion des **secrets** : credentials OAuth Discord, URLs de webhooks, `DATABASE_URL`.
- Environnements (au minimum prod ; staging souhaitable).

---

## 13. Exigences non-fonctionnelles

- **Intégrité :** opérations critiques en transaction ; journaux append-only ; stock jamais négatif.
- **Cohérence monétaire :** entiers pour les montants effectifs, `NUMERIC` pour les prix unitaires.
- **Traçabilité :** tout mouvement référence son origine (facture, session, créance, commerce) ; `activity_log` pour l'audit.
- **Sécurité :** auth centralisée Micronaut, autorisation par rôle sur chaque endpoint.
- **Langue :** interface et données en **français**.
- **Échelle :** charge modeste (serveur RP) ; pas d'optimisation prématurée.

---

## 14. Hors périmètre v1

- **Annulation / avoir de facture** (non souhaité pour l'instant ; conséquence : une facture validée par erreur ne se défait pas dans l'appli en v1).
- **Configuration dynamique des permissions** et **création de rôles** (rôles et permissions figés dans le code).
- **Autres types de business** que forge/compagnie (le modèle reste extensible, mais non livré).

---

## 15. Découpage en lots et critères de succès

> Chaque lot se termine sur un critère vérifiable (principe « goal-driven » du projet).

- **Lot 0 — Socle.** Micronaut + Postgres + Flyway + OAuth Discord + RBAC + multi-business & sélecteur.
  *Vérif :* un user se connecte via Discord, voit ses business, change de business courant ; les endpoints sont protégés par rôle.
- **Lot 1 — Catalogue & valeurs.** Items + recettes globales (avec garde-fou anti-cycle) ; valeurs par business historisées.
  *Vérif :* créer un item composé, refus d'un cycle de recette, modifier une valeur conserve l'historique.
- **Lot 2 — Comptes & journal.** Comptes/coffres + journal de mouvements ; projections stock & soldes.
  *Vérif :* une série de mouvements donne le bon stock/solde ; aucun stock négatif.
- **Lot 3 — Forge.** Sessions (prise/fin de poste), factures (brouillon → validée, prix modifiable, arrondi par excès, taxe), récap, webhooks.
  *Vérif :* une vente décrémente la marchandise, crédite les septimes, fige les prix, calcule la part business/travailleur ; webhooks émis.
- **Lot 4 — Trésorerie compagnie.** Créances (dépôts valorisés) + paiements échelonnés.
  *Vérif :* « payé X sur Y » correct après plusieurs paiements partiels.
- **Lot 5 — Commerce inter-business.** Transferts/factures entre business.
  *Vérif :* un transfert débite un business et crédite l'autre de manière cohérente.
- **Lot 6 — Statistiques.** CA, bénéfice, valeur du stock, marchandise entrée, créance restante, historiques prix/taux.
  *Vérif :* les agrégats concordent avec le journal et les tables historisées.

---

## 16. Points ouverts à trancher

- Périmètre d'écriture exact du rôle **MEMBRE** (ex. droit d'enregistrer un paiement de créance ?).
- Arrondi par excès : au niveau **ligne** ou **total** de facture ?
- Webhooks configurés **globalement** ou **par business** ?
- Timing du **Lot 5** (commerce inter-business) : v1 ou phase ultérieure ?
- API : **OpenAPI** généré côté Micronaut comme contrat front/back ?
