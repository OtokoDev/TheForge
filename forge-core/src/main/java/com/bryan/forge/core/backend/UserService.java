package com.bryan.forge.core.backend;

import com.bryan.forge.core.backend.dto.UserSummaryDto;
import com.bryan.forge.core.datamodel.GlobalRole;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datarepository.UserRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Singleton
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    /** Crée l'utilisateur s'il n'existe pas, sinon rafraîchit username/avatar depuis Discord. */
    @Transactional
    public User upsertFromDiscord(String discordId, String username, String avatar) {
        return repo.findByDiscordId(discordId)
                .map(user -> {
                    user.setUsername(username);
                    user.setAvatar(avatar);
                    return repo.update(user);
                })
                .orElseGet(() -> repo.save(new User(discordId, username, avatar)));
    }

    /** Met à jour le pseudo RP en jeu (vide → effacé). */
    @Transactional
    public User updateInGameName(UUID userId, String inGameName) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + userId));
        String trimmed = inGameName == null ? null : inGameName.strip();
        user.setInGameName(trimmed == null || trimmed.isEmpty() ? null : trimmed);
        return repo.update(user);
    }

    @Transactional
    public User setWebhooksEnabled(UUID userId, boolean enabled) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + userId));
        user.setWebhooksEnabled(enabled);
        return repo.update(user);
    }

    @Transactional
    public User setGlobalRole(UUID userId, GlobalRole role) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + userId));
        user.setGlobalRole(role);
        return repo.update(user);
    }

    /** Recherche par pseudo Discord ou nom en jeu (autocomplétion, max 25). */
    @Transactional
    public List<UserSummaryDto> search(String query) {
        String needle = query == null ? "" : query.strip().toLowerCase();
        return repo.findAll().stream()
                .filter(u -> needle.isEmpty()
                        || u.getUsername().toLowerCase().contains(needle)
                        || (u.getInGameName() != null && u.getInGameName().toLowerCase().contains(needle)))
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .limit(25)
                .map(UserSummaryDto::from)
                .toList();
    }

    @Transactional
    public User requireByDiscordId(String discordId) {
        return repo.findByDiscordId(discordId)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + discordId));
    }
}
