package com.bryan.forge.backend.realtime;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.validator.TokenValidator;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Canal temps réel unique. Auth par le cookie JWT (même origine → le navigateur l'envoie
 * dans l'upgrade WS ; pas de token en query-param contrairement à Nexis). Le client envoie
 * {@code "sub:<businessId>"} pour s'abonner aux événements de son business courant.
 */
@ServerWebSocket("/ws/events")
public class EventWebSocket {

    private final Collection<TokenValidator<HttpRequest<?>>> tokenValidators;

    public EventWebSocket(Collection<TokenValidator<HttpRequest<?>>> tokenValidators) {
        this.tokenValidators = tokenValidators;
    }

    @OnOpen
    public void onOpen(WebSocketSession session, HttpRequest<?> request) {
        String token = request.getCookies().findCookie("JWT").map(Cookie::getValue).orElse(null);
        Authentication auth = token == null ? null : authenticate(token, request);
        if (auth == null) {
            session.close(CloseReason.UNSUPPORTED_DATA);
            return;
        }
        session.put("username", auth.getName());
    }

    @OnMessage
    public void onMessage(String message, WebSocketSession session) {
        if (message != null && message.startsWith("sub:")) {
            session.put("business", message.substring(4));
        }
        // sinon : heartbeat ("ping") — rien à faire.
    }

    @OnClose
    public void onClose(WebSocketSession session) {
        // Micronaut retire la session de son registre.
    }

    private Authentication authenticate(String token, HttpRequest<?> request) {
        for (TokenValidator<HttpRequest<?>> validator : tokenValidators) {
            Authentication auth = firstOrNull(validator.validateToken(token, request));
            if (auth != null) return auth;
        }
        return null;
    }

    /** Premier élément d'un Publisher de façon bloquante, sans Reactor. */
    private static Authentication firstOrNull(Publisher<Authentication> publisher) {
        CompletableFuture<Authentication> future = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<>() {
            @Override public void onSubscribe(Subscription s) { s.request(1); }
            @Override public void onNext(Authentication a)    { future.complete(a); }
            @Override public void onError(Throwable t)        { future.complete(null); }
            @Override public void onComplete()                { future.complete(null); }
        });
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
    }
}
