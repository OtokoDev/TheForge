package com.bryan.forge.core.backend;

import com.bryan.forge.core.datarepository.UserRepository;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Denylist EN MÉMOIRE des comptes désactivés (clé = discordId = name du principal JWT).
 *
 * <p>Le JWT est stateless : désactiver un compte en base ne révoque pas le token (valide
 * jusqu'à expiration). Cette denylist, consultée par {@code ActiveCheckFilter} à chaque
 * appel /api/**, rend le ban effectif au prochain appel — sans lookup DB (O(1)).
 */
@Singleton
public class BannedRegistry {

    private final UserRepository repo;
    private final Set<String> banned = ConcurrentHashMap.newKeySet();

    public BannedRegistry(UserRepository repo) {
        this.repo = repo;
    }

    /** Charge les comptes déjà désactivés au démarrage. */
    @EventListener
    void onStartup(StartupEvent event) {
        repo.findAll().forEach(u -> {
            if (!u.isActive()) {
                banned.add(u.getDiscordId());
            }
        });
    }

    public boolean isBanned(String discordId) {
        return discordId != null && banned.contains(discordId);
    }

    /** Met à jour la denylist au moment du ban / réactivation. */
    public void set(String discordId, boolean active) {
        if (discordId == null) {
            return;
        }
        if (active) {
            banned.remove(discordId);
        } else {
            banned.add(discordId);
        }
    }
}
