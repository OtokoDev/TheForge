package com.bryan.forge.core.backend;

import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datarepository.UserRepository;
import io.micronaut.context.BeanContext;
import io.micronaut.context.event.StartupEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Denylist en mémoire du bannissement (paquet identique pour accéder à onStartup). */
class BannedRegistryTest {

    @Test
    void banPuisReactivation() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.findAll()).thenReturn(List.of());
        BannedRegistry reg = new BannedRegistry(repo);

        assertFalse(reg.isBanned("d1"));
        assertFalse(reg.isBanned(null));

        reg.set("d1", false); // ban
        assertTrue(reg.isBanned("d1"));

        reg.set("d1", true); // réactivation
        assertFalse(reg.isBanned("d1"));
    }

    @Test
    void chargeLesComptesDesactivesAuDemarrage() {
        User actif = new User("act", "Actif", null);
        User banni = new User("ban", "Banni", null);
        banni.setActive(false);
        UserRepository repo = mock(UserRepository.class);
        when(repo.findAll()).thenReturn(List.of(actif, banni));

        BannedRegistry reg = new BannedRegistry(repo);
        reg.onStartup(new StartupEvent(mock(BeanContext.class)));

        assertTrue(reg.isBanned("ban"));
        assertFalse(reg.isBanned("act"));
    }
}
