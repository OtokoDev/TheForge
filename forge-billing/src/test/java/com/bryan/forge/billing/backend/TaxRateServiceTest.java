package com.bryan.forge.billing.backend;

import com.bryan.forge.billing.backend.dto.TaxRateDto;
import com.bryan.forge.billing.datamodel.TaxRate;
import com.bryan.forge.billing.datarepository.TaxRateRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.core.datamodel.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaxRateServiceTest {

    private final TaxRateRepository repo = mock(TaxRateRepository.class);
    private final BusinessRepository businessRepo = mock(BusinessRepository.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final TaxRateService service = new TaxRateService(repo, businessRepo, access);

    private final User actor = mock(User.class);
    private final UUID biz = UUID.randomUUID();

    @Test
    void changerLeTaux_clotureLAncienEtCreeLeNouveau() {
        Business business = mock(Business.class);
        TaxRate existing = mock(TaxRate.class);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(business));
        when(repo.findByBusinessIdAndValidToIsNull(biz)).thenReturn(Optional.of(existing));
        when(repo.save(any(TaxRate.class))).thenAnswer(inv -> inv.getArgument(0));

        TaxRateDto dto = service.setRate(actor, biz, new BigDecimal("0.5"));

        assertThat(dto.rate()).isEqualByComparingTo("0.5");
        verify(existing).setValidTo(any(Instant.class));
        verify(repo).update(existing);
        verify(repo).save(any(TaxRate.class));
    }

    @Test
    void refuseTauxHorsBornes() {
        Business business = mock(Business.class);
        when(businessRepo.findById(biz)).thenReturn(Optional.of(business));

        assertThatThrownBy(() -> service.setRate(actor, biz, new BigDecimal("1.5")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }
}
