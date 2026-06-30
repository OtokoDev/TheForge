# Forge de Markarth

Application de gestion d'**économie roleplay Skyrim** : forge, échoppe, stock, ventes et
trésorerie pour les activités d'un serveur Discord. Monnaie : **septims**.

Monolithe modulaire **Micronaut** (Java) + front **Svelte 5** embarqué dans un **jar unique**
(aucun Node au runtime). Authentification **Discord OAuth2**.

## Stack

- **Back** : Micronaut 5, JDK 26 (cible release 25), PostgreSQL + Flyway, Micronaut Security
  (JWT cookie, OAuth2 Discord), WebSocket temps réel.
- **Front** : Svelte 5 + Vite, routing `svelte-spa-router` (hash), Tailwind v4. Export statique
  embarqué dans les ressources du back, servi par Micronaut.
- **Package** : `com.bryan.forge`.

## Architecture

Monolithe modulaire (Maven multi-module). Chaque module suit le découpage
`datamodel` (JPA) / `datarepository` (Micronaut Data) / `backend` (services + contrôleurs).

| Module | Rôle |
|---|---|
| `forge-core` | Entités/services transverses (User, audit, rôles, denylist ban). |
| `forge-security` | OAuth2 Discord, mapping JWT. |
| `forge-catalog` | Catalogue global : items, familles, matériaux, recettes (SYSTEM). |
| `forge-business` | Business (forge/compagnie), membres, activité. |
| `forge-valuation` | Produits par business : valeur (coût) + prix de revente (historisés). |
| `forge-ledger` | Coffres/comptes + journal de mouvements (stock, septims). |
| `forge-billing` | Facturation, sessions de service, taxe. |
| `forge-treasury` | Créances : rachat de matières aux farmeurs, paiements. |
| `forge-stats` | Statistiques (CA, marge, produits, forgerons, stock, créances…). |
| `forge-notifications` | Webhooks Discord. |
| `forge-app` | Assemblage : config, sécurité, WebSocket, **front Svelte embarqué**. |

## Pré-requis

- **JDK 26** (build en release 25). Lancer via `./mvnw` (le wrapper utilise `JAVA_HOME`).
- **PostgreSQL** local (base `theforge`).
- **Node** : géré automatiquement par le `frontend-maven-plugin` au build ; pour le dev front
  rapide, un Node local suffit.

### Base de données (dev)

```bash
psql -U postgres -c "CREATE DATABASE theforge;"
# identifiants dev dans forge-app/src/main/resources/application-dev.properties
```

## Développement

### Tout-en-un (front embarqué, servi par le back sur :8080)

```bash
./mvnw -pl forge-app -am compile     # build + embarque le front Svelte dans les ressources
# puis démarrer l'app depuis l'IDE (profil dev actif par défaut)
```

### Front en rechargement à chaud (Vite :5173)

```bash
npm --prefix forge-app/front install
npm --prefix forge-app/front run dev   # proxifie /api, /oauth, /ws vers le back :8080
```

> L'authentification (cookie OAuth) fonctionne sur l'origine embarquée `:8080`. Le serveur
> Vite `:5173` sert au dev visuel ; pour tester le flux de connexion complet, utilise `:8080`.

### Itération back seule

```bash
./mvnw -pl forge-app -am compile -Dskip.frontend=true   # ne rebuild pas le front
```

## Build

```bash
./mvnw -pl forge-app -am clean package -DskipTests
# → forge-app/target/forge-app-*.jar : jar all-in-one (front + back)
java -jar forge-app/target/forge-app-*.jar
```

Le `frontend-maven-plugin` (phase `generate-resources`) lance `vite build`, qui écrit l'export
directement dans `forge-app/src/main/resources/public` (embarqué au jar).

## Tests

```bash
./mvnw test     # tests unitaires (le groupe "integration" est exclu par défaut)
```

Les tests d'intégration (Testcontainers PostgreSQL) sont tagués `integration` et nécessitent Docker.

## Configuration (prod)

Variables d'environnement (cf. `deploy/env.example`) :

- `DATASOURCES_DEFAULT_URL/USERNAME/PASSWORD` — PostgreSQL.
- `JWT_GENERATOR_SIGNATURE_SECRET` — secret HS256 (≥ 32 caractères).
- `DISCORD_CLIENT_ID` / `DISCORD_CLIENT_SECRET` — OAuth2 Discord.
- `FORGE_OWNER_DISCORD_ID` — Discord ID promu SYSTEM à la 1re connexion (optionnel).

## Déploiement

CI/CD GitHub Actions → image Docker Hub → SSH VPS → `docker compose up -d`. Détails et fichiers
dans **[deployment.md](deployment.md)** (`Dockerfile`, `docker-compose.yaml`, Caddy, secrets).

## Carte de Bordeciel (minimap)

Carte interactive Leaflet (page `/carte`), tuiles XYZ servies en statique (`CRS.Simple`, pixels).
Fond = carte UESP (**CC-BY-SA 2.5**, crédit affiché en bas de carte ; usage fan non commercial).
Les libellés allemands d'origine sont **vectorisés** → masqués au build ; les toponymes FR sont
une surcouche data-driven.

### Régénérer les tuiles

```bash
npm --prefix forge-app/front run build:map
```

Pipeline reproductible : `map-src/skyrim_de.svg` → masque `<g id="Text">` → PNG 8192 px (resvg-js)
→ tuiles 256 px (sharp) dans `public/map/tiles/` + `public/map/meta.json`. Réédite le SVG puis
relance `build:map`.

### Marqueurs RP

`src/lib/data/markers.json` — schéma `{ id, nom_fr, type, x, y, description, faction? }`.
`type` ∈ `cite, contree, village, fort, donjon, camp, filon, chasse`. `x`/`y` = **pixels natifs**
(0..8192 × 0..5795). Les exemples fournis ont des coordonnées **placeholder à caler**.

**Relever des coordonnées** : ouvre `/carte`, clique sur la carte → la console affiche `x=… y=…`.
Reporte-les dans `markers.json`.
