package com.bryan.forge.business.backend;

import com.bryan.forge.business.backend.dto.MemberDto;
import com.bryan.forge.business.backend.dto.MembershipDto;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datamodel.Membership;
import com.bryan.forge.business.datamodel.MembershipRole;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.business.datarepository.MembershipRepository;
import com.bryan.forge.core.backend.AuditService;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datarepository.UserRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Singleton
public class MembershipService {

    private static final Logger WEB = LoggerFactory.getLogger("com.bryan.forge.web");

    private final MembershipRepository membershipRepo;
    private final BusinessRepository businessRepo;
    private final UserRepository userRepo;
    private final BusinessAccessService access;
    private final AuditService audit;

    public MembershipService(MembershipRepository membershipRepo, BusinessRepository businessRepo,
                             UserRepository userRepo, BusinessAccessService access, AuditService audit) {
        this.membershipRepo = membershipRepo;
        this.businessRepo = businessRepo;
        this.userRepo = userRepo;
        this.access = access;
        this.audit = audit;
    }

    /** Appartenances de l'utilisateur courant (pour {@code GET /api/me}). */
    @Transactional
    public List<MembershipDto> myMemberships(UUID userId) {
        return membershipRepo.findByUserId(userId).stream().map(MembershipDto::from).toList();
    }

    /** Liste les membres d'un business. Réservé à SYSTEM/STAFF ou à un membre du business. */
    @Transactional
    public List<MemberDto> listMembers(User actor, UUID businessId) {
        access.requireView(actor, businessId);
        return membershipRepo.findByBusinessId(businessId).stream().map(MemberDto::from).toList();
    }

    /** Ajoute ou met à jour le rôle d'un membre. Réservé à SYSTEM ou à un ADMIN du business. */
    @Transactional
    public MemberDto addOrUpdateMember(User actor, UUID businessId, UUID targetUserId, MembershipRole role, int version) {
        access.requireAdmin(actor, businessId);
        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        User target = userRepo.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + targetUserId));

        boolean existed = membershipRepo.findByUserIdAndBusinessId(targetUserId, businessId).isPresent();
        Membership membership = membershipRepo.findByUserIdAndBusinessId(targetUserId, businessId)
                .map(existing -> {
                    com.bryan.forge.core.backend.StaleDataException.check(existing.getVersion(), version);
                    existing.setRole(role);
                    existing.setModifiedBy(actor.getId());
                    return membershipRepo.update(existing);
                })
                .orElseGet(() -> {
                    Membership m = new Membership(target, business, role);
                    m.setCreatedBy(actor.getId());
                    m.setModifiedBy(actor.getId());
                    return membershipRepo.save(m);
                });

        String action = existed ? "ROLE_SET" : "MEMBER_ADD";
        String details = target.getUsername() + " → " + role + " @ " + business.getNom();
        audit.record(businessId, actor.getId(), action, details);
        audit.recordSystem(actor.getId(), action, details);
        WEB.info("{} : {}", action, details);
        return MemberDto.from(membership);
    }

    /** Retire un membre du business. Réservé à SYSTEM ou ADMIN du business. */
    @Transactional
    public void removeMember(User actor, UUID businessId, UUID targetUserId) {
        access.requireAdmin(actor, businessId);
        Membership membership = membershipRepo.findByUserIdAndBusinessId(targetUserId, businessId)
                .orElseThrow(() -> new NoSuchElementException("Ce membre n'appartient pas au business"));
        membershipRepo.delete(membership);
    }
}
