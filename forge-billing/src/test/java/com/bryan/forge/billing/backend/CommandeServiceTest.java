package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.CommandeDto;
import com.bryan.forge.billing.backend.dto.CreateCommandeRequest;
import com.bryan.forge.billing.backend.dto.CreateFactureLine;
import com.bryan.forge.billing.backend.dto.FactureDto;
import com.bryan.forge.billing.datamodel.Commande;
import com.bryan.forge.billing.datamodel.CommandeLine;
import com.bryan.forge.billing.datamodel.CommandeStatus;
import com.bryan.forge.billing.datamodel.FactureStatus;
import com.bryan.forge.billing.datarepository.CommandeLineRepository;
import com.bryan.forge.billing.datarepository.CommandeRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.core.datamodel.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandeServiceTest {

    private final CommandeRepository commandeRepo = mock(CommandeRepository.class);
    private final CommandeLineRepository lineRepo = mock(CommandeLineRepository.class);
    private final ItemRepository itemRepo = mock(ItemRepository.class);
    private final PricingService pricing = mock(PricingService.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final FactureService factureService = mock(FactureService.class);
    private final CommandeService service = new CommandeService(commandeRepo, lineRepo, itemRepo,
            pricing, businessRepo, access, factureService);

    private final User actor = mock(User.class);
    private final UUID biz = UUID.randomUUID();
    private final UUID cid = UUID.randomUUID();
    private final UUID itemX = UUID.randomUUID();

    @Test
    void create_enregistreCommandeEtLignes() {
        when(actor.getId()).thenReturn(UUID.randomUUID());
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));
        when(commandeRepo.nextNumero()).thenReturn(1L);
        when(commandeRepo.save(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepo.findById(itemX)).thenReturn(Optional.of(mock(Item.class)));
        when(pricing.resolveUnitPrice(any(), any(), any())).thenReturn(new BigDecimal("50"));
        when(lineRepo.findByCommandeId(any())).thenReturn(List.of());
        when(itemRepo.findAll()).thenReturn(List.of());

        CommandeDto dto = service.create(actor, biz,
                new CreateCommandeRequest("Bjorn", null, null, null,
                        List.of(new CreateFactureLine(itemX, 2, new BigDecimal("50")))));

        verify(commandeRepo).save(any(Commande.class));
        verify(lineRepo).save(any(CommandeLine.class));
        assertThat(dto.status()).isEqualTo(CommandeStatus.DEVIS);
    }

    @Test
    void setStatus_refuseSurCommandeTerminee() {
        Commande c = mock(Commande.class);
        when(c.getBusinessId()).thenReturn(biz);
        when(c.getStatus()).thenReturn(CommandeStatus.LIVREE);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));
        when(commandeRepo.findById(cid)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.setStatus(actor, biz, cid, CommandeStatus.CONFIRMEE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void convert_creeFactureBrouillonEtMarqueLivree() {
        Commande c = mock(Commande.class);
        when(c.getBusinessId()).thenReturn(biz);
        when(c.getStatus()).thenReturn(CommandeStatus.PRETE);
        when(c.getFactureId()).thenReturn(null);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(mock(Business.class)));
        when(commandeRepo.findById(cid)).thenReturn(Optional.of(c));
        CommandeLine line = mock(CommandeLine.class);
        when(line.getItemId()).thenReturn(itemX);
        when(line.getQuantity()).thenReturn(2);
        when(line.getUnitPriceSnapshot()).thenReturn(new BigDecimal("50"));
        when(lineRepo.findByCommandeId(cid)).thenReturn(List.of(line));
        UUID factureId = UUID.randomUUID();
        FactureDto facture = new FactureDto(factureId, 5L, FactureStatus.BROUILLON, false, null, 0L,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, Instant.now(), null, UUID.randomUUID(), List.of());
        when(factureService.create(any(), any(), any())).thenReturn(facture);

        FactureDto out = service.convertToFacture(actor, biz, cid);

        assertThat(out.id()).isEqualTo(factureId);
        verify(c).setFactureId(factureId);
        verify(c).setStatus(CommandeStatus.LIVREE);
        verify(commandeRepo).update(c);
    }
}
