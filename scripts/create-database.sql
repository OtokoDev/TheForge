-- ============================================================================
-- TheForge — création de la base de données applicative
--
-- À exécuter en tant que superuser PostgreSQL :
--     psql -U postgres -f scripts/create-database.sql
--
-- NB : CREATE DATABASE ne peut pas tourner dans une transaction ; lancer ce
-- fichier tel quel (pas de BEGIN/COMMIT autour). Idempotent (relançable).
-- ============================================================================

-- 1. Mot de passe + droit de connexion du superuser, cohérent avec
--    application-dev.properties (postgres / postgres).
ALTER ROLE postgres WITH LOGIN PASSWORD 'postgres';

-- 2. Base « theforge », ouverte aux connexions (CONNECTION LIMIT = -1 = illimité).
--    template0 + UTF8 évite les conflits d'encodage/collation hérités de template1.
--    Le bloc \gexec ne crée la base que si elle n'existe pas déjà.
SELECT 'CREATE DATABASE theforge WITH OWNER = postgres ENCODING = ''UTF8'' TEMPLATE = template0 CONNECTION LIMIT = -1'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'theforge')\gexec

-- 3. Fuseau horaire France : Europe/Paris (UTC+2 en été, UTC+1 en hiver, DST auto).
ALTER DATABASE theforge SET timezone TO 'Europe/Paris';

-- 4. Droits sur la base.
GRANT ALL PRIVILEGES ON DATABASE theforge TO postgres;

-- 5. Dans la base : extension fournissant gen_random_uuid() (utilisée par toutes
--    les migrations Flyway). Native sur PostgreSQL 13+, l'extension est sans effet
--    de bord ; on la garde pour rester compatible < 13.
\connect theforge
CREATE EXTENSION IF NOT EXISTS pgcrypto;
GRANT ALL ON SCHEMA public TO postgres;

\echo 'Base theforge prête (TZ Europe/Paris, postgres/postgres).'
