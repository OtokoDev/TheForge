package com.bryan.forge.backend.error;

import com.bryan.forge.backend.dto.ApiError;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.backend.StaleDataException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

/**
 * Traduit les exceptions métier en réponses HTTP propres (statut adéquat + message
 * lisible), au lieu d'un 500 générique. Le front lit simplement {@code body.message}.
 */
public final class BusinessExceptionHandlers {

    private BusinessExceptionHandlers() {}

    /** Droit insuffisant sur la ressource (authentifié mais non autorisé). */
    @Produces
    @Singleton
    @Requires(classes = {ForbiddenException.class, ExceptionHandler.class})
    public static class Forbidden implements ExceptionHandler<ForbiddenException, HttpResponse<?>> {
        @Override
        public HttpResponse<?> handle(HttpRequest request, ForbiddenException e) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(new ApiError(e.getMessage()));
        }
    }

    /** Règle métier violée (conflit, état incompatible…). */
    @Produces
    @Singleton
    @Requires(classes = {IllegalStateException.class, ExceptionHandler.class})
    public static class IllegalState implements ExceptionHandler<IllegalStateException, HttpResponse<?>> {
        @Override
        public HttpResponse<?> handle(HttpRequest request, IllegalStateException e) {
            return HttpResponse.status(HttpStatus.CONFLICT).body(new ApiError(e.getMessage()));
        }
    }

    /** Conflit de version optimiste (données modifiées entre-temps) → 409. */
    @Produces
    @Singleton
    @Requires(classes = {StaleDataException.class, ExceptionHandler.class})
    public static class Stale implements ExceptionHandler<StaleDataException, HttpResponse<?>> {
        @Override
        public HttpResponse<?> handle(HttpRequest request, StaleDataException e) {
            return HttpResponse.status(HttpStatus.CONFLICT).body(new ApiError(e.getMessage()));
        }
    }

    /** Verrou optimiste Hibernate (incrément {@code @Version}) → 409. */
    @Produces
    @Singleton
    @Requires(classes = {io.micronaut.data.exceptions.OptimisticLockException.class, ExceptionHandler.class})
    public static class OptimisticLock implements ExceptionHandler<io.micronaut.data.exceptions.OptimisticLockException, HttpResponse<?>> {
        @Override
        public HttpResponse<?> handle(HttpRequest request, io.micronaut.data.exceptions.OptimisticLockException e) {
            return HttpResponse.status(HttpStatus.CONFLICT).body(new ApiError("Données modifiées entre-temps — actualisez puis réessayez"));
        }
    }

    /** Requête invalide (argument incorrect). */
    @Produces
    @Singleton
    @Requires(classes = {IllegalArgumentException.class, ExceptionHandler.class})
    public static class IllegalArgument implements ExceptionHandler<IllegalArgumentException, HttpResponse<?>> {
        @Override
        public HttpResponse<?> handle(HttpRequest request, IllegalArgumentException e) {
            return HttpResponse.badRequest(new ApiError(e.getMessage()));
        }
    }

    /** Ressource introuvable. */
    @Produces
    @Singleton
    @Requires(classes = {NoSuchElementException.class, ExceptionHandler.class})
    public static class NotFound implements ExceptionHandler<NoSuchElementException, HttpResponse<?>> {
        @Override
        public HttpResponse<?> handle(HttpRequest request, NoSuchElementException e) {
            return HttpResponse.status(HttpStatus.NOT_FOUND).body(new ApiError(e.getMessage()));
        }
    }

    /**
     * Filet de sécurité : toute exception non gérée → 500 + log de la stack trace
     * complète côté serveur.
     */
    @Produces
    @Singleton
    @Requires(classes = {RuntimeException.class, ExceptionHandler.class})
    public static class Generic implements ExceptionHandler<RuntimeException, HttpResponse<?>> {
        private static final Logger LOG = LoggerFactory.getLogger(Generic.class);
        @Override
        public HttpResponse<?> handle(HttpRequest request, RuntimeException e) {
            LOG.error("Erreur non gérée sur {} {} : {}", request.getMethod(), request.getPath(), e.getMessage(), e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return HttpResponse.serverError(new ApiError(msg));
        }
    }
}
