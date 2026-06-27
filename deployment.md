# Déploiement TheForge (CI/CD)

Pipeline : **push sur `main`** → GitHub Actions build l'image Docker (front + back en un jar)
→ push sur **Docker Hub** → **SSH sur le VPS** → `docker compose pull && up -d`.
App exposée sur l'hôte **:6000**, servie par **Cloudflare** et/ou le **Caddy existant**. APM **Glowroot**.

---

## 1. Fichiers du dépôt

| Fichier | Rôle |
|---|---|
| `Dockerfile` | Build multi-stage (JDK 26) : front Next + back Micronaut → fat jar ; runtime JRE non-root, healthcheck `/health`, logs tee'és. |
| `.dockerignore` | Exclut les artefacts locaux du contexte de build. |
| `docker-compose.yaml` | Service `app`, DB = Postgres **hôte**, port **6000**, agent Glowroot, volume logs. **Le seul compose déployé** (dans `/home/forge/theforge`). |
| `docker-compose.override.yaml` | **LOCAL uniquement** (build + port 8081). **À NE PAS copier sur le VPS.** |
| `deploy/env.example` | Modèle du `.env` de prod (→ `/home/forge/theforge/.env`). |
| `deploy/Caddyfile.theforge` | Caddyfile fusionné (nexis inchangé + bloc TheForge) — ou bloc à ajouter. |
| `.github/workflows/ci.yml` | Build + compile des tests sur chaque push/PR. |
| `.github/workflows/release.yml` | Build/push image + déploiement SSH sur push `main`. |

---

## 2. Secrets GitHub (Settings → Secrets and variables → Actions)

| Secret | Valeur |
|---|---|
| `DOCKERHUB_USERNAME` | `bryanguerin` |
| `DOCKERHUB_TOKEN` | jeton Docker Hub |
| `VPS_HOST` | IP/host du VPS |
| `VPS_USER` | `forge` |
| `VPS_SSH_KEY` | clé privée SSH du user `forge` |
| `VPS_PORT` | (optionnel) port SSH, défaut 22 |
| `VPS_PATH` | `/home/forge/theforge` |

---

## 3. Préparation du VPS (une seule fois)

### 3.1 User `forge`
```bash
sudo useradd -m -s /bin/bash forge        # crée /home/forge
sudo usermod -aG docker forge             # accès Docker (pour le déploiement SSH)
sudo mkdir -p /home/forge/.ssh && sudo nano /home/forge/.ssh/authorized_keys   # clé publique CI
sudo chown -R forge:forge /home/forge/.ssh && sudo chmod 700 /home/forge/.ssh && sudo chmod 600 /home/forge/.ssh/authorized_keys
id forge                                   # note UID/GID (PUID/PGID du .env)
```

### 3.2 Dossiers + droits
```bash
sudo mkdir -p /home/forge/theforge /opt/forge /opt/volumes/forge/log /opt/glowroot
sudo chown -R forge:forge /home/forge/theforge /opt/forge /opt/volumes/forge /opt/glowroot
```

### 3.3 Glowroot (APM)
Dépose le jar de l'agent dans **`/opt/glowroot/glowroot.jar`** (le dossier `/opt/glowroot`
doit être inscriptible par `forge` : l'agent y écrit `glowroot/data` + config).
```bash
sudo chown -R forge:forge /opt/glowroot
```
L'agent est déjà branché (`-javaagent:/opt/glowroot/glowroot.jar` dans `JAVA_OPTS`).
UI Glowroot : `http://127.0.0.1:4000` (port lié au localhost de l'hôte → tunnel SSH
`ssh -L 4000:127.0.0.1:4000 forge@<vps>` ou Cloudflare Access pour y accéder).

### 3.4 Base de données (Postgres système, partagé avec nexis)
```bash
sudo -u postgres psql -c "CREATE DATABASE theforge;"
sudo -u postgres psql -c "CREATE USER theforge WITH PASSWORD '...';"
sudo -u postgres psql -c "GRANT ALL ON DATABASE theforge TO theforge;"
sudo -u postgres psql -d theforge -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
```
> Postgres doit accepter les connexions Docker (`listen_addresses='*'` + `pg_hba.conf`
> pour le sous-réseau Docker). Flyway applique les migrations au 1er démarrage.

### 3.5 Compose + .env
```bash
cd /home/forge/theforge
# copie UNIQUEMENT docker-compose.yaml (pas l'override) :
scp ... docker-compose.yaml forge@<vps>:/home/forge/theforge/
cp deploy/env.example .env    # puis édite .env (secrets, mot de passe DB, PUID/PGID…)
```

### 3.6 Reverse-proxy (au choix)
- **Cloudflare** (ton ami) → proxifie le domaine vers `VPS:6000`. **Rien à faire côté Caddy.**
- **Caddy** (le tien, existant) → ajoute le bloc `TheForge` de `deploy/Caddyfile.theforge`
  à ton Caddyfile, **sans toucher** au bloc nexis. Le conteneur Caddy joint l'app sur le
  port hôte 6000 → ajoute à son service :
  ```yaml
  extra_hosts:
    - "host.docker.internal:host-gateway"
  ```
  puis recharge : `docker exec <caddy> caddy reload --config /etc/caddy/Caddyfile`.

### 3.7 Discord OAuth
Developer Portal → OAuth2 → Redirects :
```
https://forge.mondomaine.fr/oauth/callback/discord
```

---

## 4. Premier déploiement (manuel)
```bash
cd /home/forge/theforge
docker compose pull
docker compose up -d
docker compose logs -f app   # Flyway OK, "Server Running", healthcheck UP
```

---

## 5. Déploiements suivants (auto)
Push/merge sur `main` → CI build + push `bryanguerin/theforge:latest` → SSH `compose pull && up -d`.
Rollback : `THEFORGE_IMAGE=bryanguerin/theforge:sha-<commit>` dans `.env` puis `docker compose up -d`.

---

## 6. Dépannage
- **compose veut build sur le VPS** → l'override a été copié. Le supprimer.
- **DB injoignable** → `host.docker.internal` / `pg_hba.conf` / `listen_addresses`. Healthcheck DOWN.
- **502 via Caddy** → conteneur Caddy sans `host.docker.internal` (host-gateway), ou app non démarrée.
- **redirect_uri OAuth invalide** → callback non déclaré, ou domaine ≠ enregistré.
- **Glowroot vide** → `/opt/glowroot` non inscriptible par `forge`, ou jar absent.
- **Logs** : `docker compose logs -f app` ou `/opt/volumes/forge/log/theforge-stdout.log`.
