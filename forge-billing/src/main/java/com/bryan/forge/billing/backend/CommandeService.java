package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CommandeDto;
import com.bryan.forge.billing.backend.dto.CommandeLineDto;
import com.bryan.forge.billing.backend.dto.CreateCommandeRequest;
import com.bryan.forge.billing.backend.dto.CreateFactureLine;
import com.bryan.forge.billing.backend.dto.CreateFactureRequest;
import com.bryan.forge.billing.backend.dto.FactureDto;
import com.bryan.forge.billing.datamodel.Commande;
import com.bryan.forge.billing.datamodel.CommandeLine;
import com.bryan.forge.billing.datamodel.CommandeStatus;
import com.bryan.forge.billing.datarepository.CommandeLineRepository;
import com.bryan.forge.billing.datarepository.CommandeRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.MovementType;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Commandes client : devis → … → livraison. À la livraison, la commande est convertie en
 * facture BROUILLON (réutilise {@link FactureService#create}) que le forgeron valide ensuite.
 */
@Singleton
public class CommandeService {

    private final CommandeRepository commandeRepo;
    private final CommandeLineRepository lineRepo;
    private final ItemRepository itemRepo;
    private final PricingService pricing;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;
    private final FactureService factureService;
    private final LedgerService ledger;

    public CommandeService(CommandeRepository commandeRepo, CommandeLineRepository lineRepo,
                           ItemRepository itemRepo, PricingService pricing,
                           BusinessRepository businessRepo, BusinessAccessService access,
                           FactureService factureService, LedgerService ledger) {
        this.commandeRepo = commandeRepo;
        this.lineRepo = lineRepo;
        this.itemRepo = itemRepo;
        this.pricing = pricing;
        this.businessRepo = businessRepo;
        this.access = access;
        this.factureService = factureService;
        this.ledger = ledger;
    }

    @Transactional
    public List<CommandeDto> list(User actor, UUID businessId) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        Map<UUID, String> names = itemNames();
        return commandeRepo.findByBusinessIdOrderByNumeroDesc(businessId).stream()
                .map(c -> toDto(c, lineRepo.findByCommandeId(c.getId()), names))
                .toList();
    }

    @Transactional
    public CommandeDto get(User actor, UUID businessId, UUID id) {
        requireBusiness(businessId);
        access.requireView(actor, businessId);
        Commande c = require(id, businessId);
        return toDto(c, lineRepo.findByCommandeId(id), itemNames());
    }

    @Transactional
    public CommandeDto create(User actor, UUID businessId, CreateCommandeRequest req) {
        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        access.requireOperate(actor, businessId);
        requireLines(req.lines());
        long acompte = req.acompte() == null ? 0 : Math.max(0, req.acompte());
        Commande c = new Commande(businessId, commandeRepo.nextNumero(), actor.getId(),
                blankToNull(req.clientName()), blankToNull(req.clientNote()), req.dueDate(), acompte);
        Commande saved = commandeRepo.save(c);
        saveLines(saved.getId(), businessId, req.lines());
        // Acompte versé par le client → encaissé au coffre dès la commande.
        if (acompte > 0) {
            UUID coffre = business.getDefaultCoffreAccountId();
            if (coffre == null) {
                throw new IllegalStateException("Aucun coffre par défaut : impossible d'encaisser l'acompte (configure un coffre)");
            }
            ledger.applyMovement(businessId, septimeId(), (int) acompte, null, coffre,
                    MovementType.DEPOSIT, "COMMANDE", saved.getId(),
                    "Acompte commande #" + saved.getNumero(), actor.getId());
        }
        return toDto(saved, lineRepo.findByCommandeId(saved.getId()), itemNames());
    }

    /** Mise à jour d'un devis (lignes + infos client). Uniquement au statut DEVIS. */
    @Transactional
    public CommandeDto update(User actor, UUID businessId, UUID id, CreateCommandeRequest req) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        Commande c = require(id, businessId);
        if (c.getStatus() != CommandeStatus.DEVIS) {
            throw new IllegalStateException("Seul un devis est modifiable");
        }
        requireLines(req.lines());
        c.setClientName(blankToNull(req.clientName()));
        c.setClientNote(blankToNull(req.clientNote()));
        c.setDueDate(req.dueDate());
        // Ajustement d'acompte : le delta est encaissé/remboursé au coffre (sinon l'acompte
        // stocké divergerait de l'argent réellement banké et la facture déduirait du vent).
        if (req.acompte() != null) {
            long newAcompte = Math.max(0, req.acompte());
            long delta = newAcompte - c.getAcompte();
            if (delta != 0) {
                Business business = businessRepo.findById(businessId)
                        .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
                UUID coffre = business.getDefaultCoffreAccountId();
                if (coffre == null) {
                    throw new IllegalStateException("Aucun coffre par défaut : impossible d'ajuster l'acompte");
                }
                if (delta > 0) {
                    ledger.applyMovement(businessId, septimeId(), (int) delta, null, coffre,
                            MovementType.DEPOSIT, "COMMANDE", c.getId(),
                            "Acompte commande #" + c.getNumero() + " (ajustement)", actor.getId());
                } else {
                    ledger.applyMovement(businessId, septimeId(), (int) -delta, coffre, null,
                            MovementType.WITHDRAWAL, "COMMANDE", c.getId(),
                            "Remboursement partiel acompte #" + c.getNumero(), actor.getId());
                }
                c.setAcompte(newAcompte);
            }
        }
        commandeRepo.update(c);
        lineRepo.deleteByCommandeId(id);
        saveLines(id, businessId, req.lines());
        return toDto(c, lineRepo.findByCommandeId(id), itemNames());
    }

    @Transactional
    public CommandeDto setStatus(User actor, UUID businessId, UUID id, CommandeStatus status) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        if (status == null) throw new IllegalArgumentException("Statut requis");
        Commande c = require(id, businessId);
        if (c.getStatus() == CommandeStatus.LIVREE || c.getStatus() == CommandeStatus.ANNULEE) {
            throw new IllegalStateException("Commande terminée — statut figé");
        }
        // Annulation → l'acompte encaissé est remboursé au client.
        if (status == CommandeStatus.ANNULEE && c.getAcompte() > 0) {
            refundAcompte(actor, businessId, c);
        }
        c.setStatus(status);
        commandeRepo.update(c);
        return toDto(c, lineRepo.findByCommandeId(id), itemNames());
    }

    /** Livraison : crée une facture BROUILLON depuis les lignes, marque la commande LIVREE. */
    @Transactional
    public FactureDto convertToFacture(User actor, UUID businessId, UUID id) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        Commande c = require(id, businessId);
        if (c.getFactureId() != null) throw new IllegalStateException("Commande déjà facturée");
        if (c.getStatus() == CommandeStatus.ANNULEE) throw new IllegalStateException("Commande annulée");
        List<CommandeLine> lines = lineRepo.findByCommandeId(id);
        if (lines.isEmpty()) throw new IllegalStateException("Commande sans ligne");

        CreateFactureRequest req = new CreateFactureRequest(
                lines.stream()
                        .map(l -> new CreateFactureLine(l.getItemId(), l.getQuantity(), l.getUnitPriceSnapshot()))
                        .toList(),
                c.getClientName(), c.getClientNote());
        FactureDto facture = factureService.create(actor, businessId, req, c.getAcompte());

        c.setFactureId(facture.id());
        c.setStatus(CommandeStatus.LIVREE);
        commandeRepo.update(c);
        return facture;
    }

    @Transactional
    public void delete(User actor, UUID businessId, UUID id) {
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        Commande c = require(id, businessId);
        if (c.getFactureId() != null) {
            throw new IllegalStateException("Commande facturée — non supprimable");
        }
        if (c.getAcompte() > 0) {
            refundAcompte(actor, businessId, c);
        }
        lineRepo.deleteByCommandeId(id);
        commandeRepo.delete(c);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void saveLines(UUID commandeId, UUID businessId, List<CreateFactureLine> lines) {
        for (CreateFactureLine line : lines) {
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("Quantité invalide");
            }
            itemRepo.findById(line.itemId())
                    .orElseThrow(() -> new NoSuchElementException("Item introuvable : " + line.itemId()));
            BigDecimal price = pricing.resolveUnitPrice(businessId, line.itemId(), line.unitPrice());
            lineRepo.save(new CommandeLine(commandeId, line.itemId(), line.quantity(), price));
        }
    }

    private void requireLines(List<CreateFactureLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Une commande doit avoir au moins une ligne");
        }
    }

    /** Rembourse l'acompte encaissé (sortie coffre) — annulation/suppression de commande. */
    private void refundAcompte(User actor, UUID businessId, Commande c) {
        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Business introuvable : " + businessId));
        UUID coffre = business.getDefaultCoffreAccountId();
        if (coffre == null) {
            throw new IllegalStateException("Aucun coffre par défaut : impossible de rembourser l'acompte");
        }
        ledger.applyMovement(businessId, septimeId(), (int) c.getAcompte(), coffre, null,
                MovementType.WITHDRAWAL, "COMMANDE", c.getId(),
                "Remboursement acompte #" + c.getNumero(), actor.getId());
    }

    private UUID septimeId() {
        return itemRepo.findFirstBySystemTrue()
                .orElseThrow(() -> new IllegalStateException("Item septime introuvable")).getId();
    }

    private void requireBusiness(UUID businessId) {
        if (businessRepo.findById(businessId).isEmpty()) {
            throw new NoSuchElementException("Business introuvable : " + businessId);
        }
    }

    private Commande require(UUID id, UUID businessId) {
        Commande c = commandeRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Commande introuvable : " + id));
        if (!c.getBusinessId().equals(businessId)) {
            throw new ForbiddenException("Cette commande n'appartient pas au business");
        }
        return c;
    }

    private Map<UUID, String> itemNames() {
        return itemRepo.findAll().stream().collect(Collectors.toMap(Item::getId, Item::getName));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private CommandeDto toDto(Commande c, List<CommandeLine> lines, Map<UUID, String> names) {
        List<CommandeLineDto> lineDtos = lines.stream().map(l -> {
            long lineTotal = l.getUnitPriceSnapshot()
                    .multiply(BigDecimal.valueOf(l.getQuantity()))
                    .setScale(0, RoundingMode.CEILING)
                    .longValueExact();
            return new CommandeLineDto(l.getId(), l.getItemId(), names.getOrDefault(l.getItemId(), "?"),
                    l.getQuantity(), l.getUnitPriceSnapshot(), lineTotal);
        }).toList();
        long total = lineDtos.stream().mapToLong(CommandeLineDto::lineTotal).sum();
        return new CommandeDto(c.getId(), c.getNumero(), c.getStatus(), c.getClientName(), c.getClientNote(),
                c.getDueDate(), c.getAcompte(), c.getFactureId(), total, c.getCreatedBy(), c.getCreatedAt(), lineDtos);
    }
}
