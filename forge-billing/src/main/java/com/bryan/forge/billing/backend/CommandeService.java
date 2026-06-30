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
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.backend.ForbiddenException;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.valuation.datamodel.Product;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
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
    private final ProductRepository productRepo;
    private final BusinessRepository businessRepo;
    private final BusinessAccessService access;
    private final FactureService factureService;

    public CommandeService(CommandeRepository commandeRepo, CommandeLineRepository lineRepo,
                           ItemRepository itemRepo, ProductRepository productRepo,
                           BusinessRepository businessRepo, BusinessAccessService access,
                           FactureService factureService) {
        this.commandeRepo = commandeRepo;
        this.lineRepo = lineRepo;
        this.itemRepo = itemRepo;
        this.productRepo = productRepo;
        this.businessRepo = businessRepo;
        this.access = access;
        this.factureService = factureService;
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
        requireBusiness(businessId);
        access.requireOperate(actor, businessId);
        requireLines(req.lines());
        Commande c = new Commande(businessId, commandeRepo.nextNumero(), actor.getId(),
                blankToNull(req.clientName()), blankToNull(req.clientNote()), req.dueDate(),
                req.acompte() == null ? 0 : Math.max(0, req.acompte()));
        Commande saved = commandeRepo.save(c);
        saveLines(saved.getId(), businessId, req.lines());
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
        if (req.acompte() != null) c.setAcompte(Math.max(0, req.acompte()));
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
        FactureDto facture = factureService.create(actor, businessId, req);

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
            BigDecimal price = line.unitPrice() != null && line.unitPrice().signum() >= 0
                    ? line.unitPrice()
                    : productRepo.findByBusinessIdAndItemIdAndValidToIsNull(businessId, line.itemId())
                            .map(Product::getPrixRevente).filter(Objects::nonNull).orElse(BigDecimal.ZERO);
            lineRepo.save(new CommandeLine(commandeId, line.itemId(), line.quantity(), price));
        }
    }

    private void requireLines(List<CreateFactureLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Une commande doit avoir au moins une ligne");
        }
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
