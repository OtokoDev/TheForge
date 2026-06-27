package com.bryan.forge.business.backend;

import com.bryan.forge.business.datamodel.MembershipRole;
import com.bryan.forge.business.datarepository.MembershipRepository;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.datamodel.GlobalRole;
import com.bryan.forge.core.datamodel.User;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.UUID;

/**
 * Autorisation par business, réutilisable par tous les contextes scopés business
 * (membres, valuations, ledger…). Règle CDC §5 : SYSTEM partout ; STAFF lecture
 * partout ; ADMIN/MEMBRE selon l'appartenance.
 */
@Singleton
public class BusinessAccessService {

    private final MembershipRepository membershipRepo;

    public BusinessAccessService(MembershipRepository membershipRepo) {
        this.membershipRepo = membershipRepo;
    }

    @Transactional
    public boolean canAdmin(User user, UUID businessId) {
        if (user.getGlobalRole() == GlobalRole.SYSTEM) return true;
        return membershipRepo.findByUserIdAndBusinessId(user.getId(), businessId)
                .map(m -> m.getRole() == MembershipRole.ADMIN)
                .orElse(false);
    }

    @Transactional
    public boolean canView(User user, UUID businessId) {
        if (user.getGlobalRole() == GlobalRole.SYSTEM || user.getGlobalRole() == GlobalRole.STAFF) return true;
        return membershipRepo.findByUserIdAndBusinessId(user.getId(), businessId).isPresent();
    }

    /** Peut effectuer des opérations dans le business (SYSTEM ou membre ADMIN/MEMBRE).
     *  Exclut STAFF (lecture seule). */
    @Transactional
    public boolean canOperate(User user, UUID businessId) {
        if (user.getGlobalRole() == GlobalRole.SYSTEM) return true;
        return membershipRepo.findByUserIdAndBusinessId(user.getId(), businessId).isPresent();
    }

    public void requireAdmin(User user, UUID businessId) {
        if (!canAdmin(user, businessId)) {
            throw new ForbiddenException("Réservé à un administrateur de ce business");
        }
    }

    public void requireOperate(User user, UUID businessId) {
        if (!canOperate(user, businessId)) {
            throw new ForbiddenException("Réservé aux membres de ce business");
        }
    }

    public void requireView(User user, UUID businessId) {
        if (!canView(user, businessId)) {
            throw new ForbiddenException("Accès refusé à ce business");
        }
    }
}
