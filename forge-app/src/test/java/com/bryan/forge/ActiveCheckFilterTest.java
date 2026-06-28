package com.bryan.forge;

import com.bryan.forge.core.backend.BannedRegistry;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveCheckFilterTest {

    private HttpRequest<?> requestAs(String discordId) {
        HttpRequest<?> req = mock(HttpRequest.class);
        Principal p = () -> discordId;
        when(req.getUserPrincipal()).thenReturn(Optional.of(p));
        return req;
    }

    @Test
    void bloque401SiBanni() {
        BannedRegistry reg = mock(BannedRegistry.class);
        when(reg.isBanned("d1")).thenReturn(true);

        HttpResponse<?> res = new ActiveCheckFilter(reg).check(requestAs("d1"));

        assertNotNull(res);
        assertEquals(401, res.getStatus().getCode());
    }

    @Test
    void laissePasserSiActif() {
        BannedRegistry reg = mock(BannedRegistry.class);
        when(reg.isBanned(anyString())).thenReturn(false);

        assertNull(new ActiveCheckFilter(reg).check(requestAs("d1")));
    }

    @Test
    void laissePasserSiAnonyme() {
        BannedRegistry reg = mock(BannedRegistry.class);
        HttpRequest<?> req = mock(HttpRequest.class);
        when(req.getUserPrincipal()).thenReturn(Optional.empty());

        assertNull(new ActiveCheckFilter(reg).check(req));
    }
}
