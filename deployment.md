# Déploiement TheForge (CI/CD)

Pipeline : **push sur `main`** → GitHub Actions build l'image Docker (front + back en un jar)
→ push sur **Docker Hub** → **SSH sur le VPS** → `docker compose pull && up -d`.
Le **Caddy existant** (celui de nexis) sert l'app via un réseau Docker partagé.

---

## 1. Fichiers du dépôt

| Fichier | Rôle |
|---|---|
| `Dockerfile` | Build multi-stage (JDK 26) : front Next + back Micronaut → fat jar ; runtime JRE non-root, healthcheck `/health`, logs tee'és. |
| `.dockerignore` | Exclut les artefacts locaux du contexte de build. |
| `docker-compose.yaml` | Service `app` (image Docker Hub), DB = Postgres **hôte**, réseau partagé `proxy`, volume logs. **Le seul compose déployé sur le VPS.** |
| `docker-compose.override.yaml` | **LOCAL uniquement** (build + port 8081). **À NE PAS copier sur le VPS.** |
| `deploy/env.example` | Modèle du `.env` de prod (à copier dans `/opt/theforge/.env`). |
| `deploy/Caddyfile.theforge` | Bloc à ajouter au Caddyfile existant. |
| `.github/workflows/ci.yml` | Build + compile des tests sur chaque push/PR. |
| `.github/workflows/release.yml` | Build/push image + déploiement SSH sur push `main`. |

---

## 2. Secrets GitHub (Settings → Secrets and variables → Actions)

| Secret | Exemple |
|---|---|
| `DOCKERHUB_USERNAME` | `bryanguerin` |
| `DOCKERHUB_TOKEN` | jeton d'accès Docker Hub (Account → Security → New Access Token) |
| `VPS_HOST` | IP/host du VPS |
| `VPS_USER` | user SSH (ex. `debian`) |
| `VPS_SSH_KEY` | clé privée SSH (PEM) autorisée sur le VPS |
| `VPS_PORT` | (optionnel) port SSH, défaut 22 |
| `VPS_PATH` | `/opt/theforge` |

---

## 3. Préparation du VPS (une seule fois)

### 3.1 Base de données (Postgres système, partagé avec nexis)
Crée la base `theforge` + l'extension `pgcrypto` sur le Postgres de l'hôte :
```bash
sudo -u postgres psql -f /chemin/scripts/create-database.sql
# ou manuellement :
sudo -u postgres psql -c "CREATE DATABASE theforge;"
sudo -u postgres psql -c "CREATE USER theforge WITH PASSWORD '...';"
sudo -u postgres psql -c "GRANT ALL ON DATABASE theforge TO theforge;"
sudo -u postgres psql -d theforge -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
```
> Postgres doit accepter les connexions depuis Docker (`listen_addresses='*'` + ligne
> `pg_hba.conf` pour le sous-réseau Docker, ou `host all all 172.16.0.0/12 scram-sha-256`).
> Les migrations Flyway s'appliquent automatiquement au 1er démarrage.

### 3.2 Dossiers + droits
```bash
sudo mkdir -p /opt/theforge /opt/volumes/theforge/log
sudo chown -R $(id -u):$(id -g) /opt/volumes/theforge
```

### 3.3 Réseau Docker partagé avec Caddy
```bash
docker network create proxy
docker network connect proxy <nom-du-conteneur-caddy>   # ex. nexis-caddy-1
```
> Vérifie le nom : `docker ps --format '{{.Names}}' | grep -i caddy`.
> Le conteneur Caddy doit rester connecté à `proxy` (re-connecter après un recreate,
> ou ajouter `proxy` (external) aux networks de son compose).

### 3.4 Compose + .env
```bash
cd /opt/theforge
# copie UNIQUEMENT docker-compose.yaml (pas l'override) :
scp ... docker-compose.yaml /opt/theforge/
cp deploy/env.example .env   # puis édite .env (secrets, mot de passe DB…)
```

### 3.5 Caddy
Ajoute le bloc `deploy/Caddyfile.theforge` au Caddyfile existant (remplace le domaine),
puis recharge Caddy :
```bash
docker exec <conteneur-caddy> caddy reload --config /etc/caddy/Caddyfile
```

### 3.6 Discord OAuth
Dans le Discord Developer Portal → ton app → OAuth2 → Redirects, ajoute :
```
https://theforge.mondomaine.fr/oauth/callback/discord
```

---

## 4. Premier déploiement (manuel)
```bash
cd /opt/theforge
docker compose pull
docker compose up -d
docker compose logs -f app   # vérifie : Flyway OK, "Server Running", healthcheck UP
```
Puis ouvre `https://theforge.mondomaine.fr` et connecte-toi via Discord
(le `FORGE_OWNER_DISCORD_ID` est promu SYSTEM à la 1re connexion).

---

## 5. Déploiements suivants (auto)
Chaque **push/merge sur `main`** : la CI build + push l'image `bryanguerin/theforge:latest`
puis SSH → `docker compose pull && up -d`. Rien à faire.

Rollback : `docker compose` avec `THEFORGE_IMAGE=bryanguerin/theforge:sha-<commit>` dans `.env`,
puis `docker compose up -d`.

---

## 6. Dépannage
- **`docker compose` veut build sur le VPS** → l'`override` a été copié par erreur. Le supprimer.
- **DB injoignable** → `host.docker.internal` non résolu / `pg_hba.conf` / `listen_addresses`. Healthcheck DOWN.
- **Caddy 502** → conteneur Caddy pas sur le réseau `proxy`, ou `theforge-app` non démarré.
- **redirect_uri OAuth invalide** → callback non déclaré côté Discord, ou domaine ≠ celui enregistré.
- **Logs** : `docker compose logs -f app` (console) ou `/opt/volumes/theforge/log/theforge-stdout.log`.
