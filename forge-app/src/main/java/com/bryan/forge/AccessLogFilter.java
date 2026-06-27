package com.bryan.forge;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.security.authentication.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Journal d'accès HTTP au format Nexis : {@code MÉTHODE /uri >> OK (200) 12ms}. Le pseudo
 * de l'utilisateur est posé en MDC (« user ») le temps de la ligne. 401/403 → REFUSÉ (WARN).
 */
@ServerFilter("/api/**")
public class AccessLogFilter {

    private static final Logger LOG = LoggerFactory.getLogger("com.bryan.forge.web");
    private static final String START = "forge.req.start";

    @RequestFilter
    public void onRequest(HttpRequest<?> request) {
        request.setAttribute(START, System.nanoTime());
    }

    @ResponseFilter
    public void onResponse(HttpRequest<?> request, HttpResponse<?> response) {
        long startNs = request.getAttribute(START, Long.class).orElse(System.nanoTime());
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        int status = response.getStatus().getCode();
        // Le principal porte le discordId ; on affiche le pseudo (attribut "username").
        String user = request.getUserPrincipal()
                .map(p -> p instanceof Authentication a ? String.valueOf(a.getAttributes().getOrDefault("username", a.getName())) : p.getName())
                .orElse("");

        String method = request.getMethodName();
        String uri = request.getUri().getPath();
        String verdict = status == 401 || status == 403 ? "REFUSÉ" : status >= 400 ? "ERREUR" : "OK";
        String suffix = user.isEmpty() ? " [anonyme]" : "";

        MDC.put("user", user);
        try {
            String msg = "{} {} >> {} ({}) {}ms{}";
            if (status >= 400) {
                LOG.warn(msg, method, uri, verdict, status, ms, suffix);
            } else {
                LOG.info(msg, method, uri, verdict, status, ms, suffix);
            }
        } finally {
            MDC.remove("user");
        }
    }
}
