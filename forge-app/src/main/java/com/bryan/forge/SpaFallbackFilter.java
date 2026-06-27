package com.bryan.forge;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Fallback SPA pour l'export statique Next embarqué dans le jar.
 *
 * <p>Le resolver statique de Micronaut, sur une route comme {@code /dashboard}, fait
 * {@code getResource("dashboard")} : dans un jar, l'ENTRÉE répertoire {@code public/dashboard/}
 * existe → il la « sert » comme un flux vide → réponse 200 sans corps (page blanche). Le
 * fallback {@code dashboard/index.html} n'est jamais atteint. Ça casse toutes les sous-routes
 * dès que le front est packagé (≠ dev).
 *
 * <p>Ce filtre intercepte les requêtes de navigation (GET, sans extension, hors API/asset) et
 * renvoie explicitement le {@code index.html} de la route (sinon celui de la racine : le routeur
 * client Next prend alors le relais).
 */
@ServerFilter("/**")
public class SpaFallbackFilter {

    @RequestFilter
    @Nullable
    public HttpResponse<byte[]> serveSpa(HttpRequest<?> request) {
        if (request.getMethod() != HttpMethod.GET) {
            return null;
        }
        String path = request.getUri().getPath();
        // API, flux OAuth/logout, WebSocket, assets Next, health : pipeline normal.
        if (path.startsWith("/api") || path.startsWith("/oauth") || path.startsWith("/logout")
                || path.startsWith("/ws") || path.startsWith("/_next") || path.startsWith("/health")) {
            return null;
        }
        // Un point dans le dernier segment = vrai fichier (.js/.css/.svg/.ico/.html/.txt) → laisser passer.
        String last = path.substring(path.lastIndexOf('/') + 1);
        if (last.indexOf('.') >= 0) {
            return null;
        }

        String rel = path.replaceAll("^/+", "").replaceAll("/+$", "");
        byte[] html = load(rel.isEmpty() ? "public/index.html" : "public/" + rel + "/index.html");
        if (html == null) {
            html = load("public/index.html");
        }
        if (html == null) {
            return null; // export absent : laisser le pipeline répondre (404).
        }
        return HttpResponse.ok(html).contentType("text/html; charset=utf-8");
    }

    private byte[] load(String resource) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}
