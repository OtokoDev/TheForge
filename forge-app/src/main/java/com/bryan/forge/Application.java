package com.bryan.forge;

import io.micronaut.runtime.Micronaut;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Application {

    public static void main(String[] args) {
        // Micronaut ne lit pas les .env nativement : on charge le .env (s'il existe)
        // en propriétés système AVANT de démarrer, pour que ${DISCORD_CLIENT_ID} etc.
        // se résolvent. En prod/docker, les vraies variables d'env priment (non écrasées).
        loadDotEnv();

        // Environnement "dev" par défaut : charge application-dev.properties (PostgreSQL local).
        // Ignoré si un environnement est explicitement défini — `test` activé par @MicronautTest,
        // `prod` via MICRONAUT_ENVIRONMENTS dans le docker-compose de prod.
        Micronaut.build(args)
                .mainClass(Application.class)
                .defaultEnvironments("dev")
                .start();
    }

    /**
     * Cherche un fichier {@code .env} depuis le répertoire courant en remontant (jusqu'à
     * 4 niveaux : couvre un lancement depuis la racine du repo OU depuis forge-app), et
     * pose chaque {@code CLE=VALEUR} en propriété système si la clé n'est pas déjà définie
     * (variable d'env ou propriété système existante = prioritaire). Fichier optionnel.
     */
    private static void loadDotEnv() {
        Path dir = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && dir != null; depth++, dir = dir.getParent()) {
            Path candidate = dir.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                applyDotEnv(candidate);
                return;
            }
        }
    }

    private static void applyDotEnv(Path envFile) {
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                int eq = trimmed.indexOf('=');
                if (eq <= 0) continue;

                String key = trimmed.substring(0, eq).strip();
                String value = trimmed.substring(eq + 1).strip();
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }

                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
            System.out.println("[forge] .env chargé depuis " + envFile);
        } catch (IOException e) {
            System.err.println("[forge] Lecture du .env impossible (" + envFile + ") : " + e.getMessage());
        }
    }
}
