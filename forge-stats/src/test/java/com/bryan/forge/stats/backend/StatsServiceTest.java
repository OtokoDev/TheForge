package com.bryan.forge.stats.backend;

import com.bryan.forge.billing.datamodel.Facture;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class StatsServiceTest {

    private static Facture facture(long total, long deposit, boolean paid) {
        Facture f = new Facture(UUID.randomUUID(), 1L, UUID.randomUUID(), null, null);
        f.setTotalAmount(total);
        f.setDeposit(deposit);
        f.setPaid(paid);
        return f;
    }

    @Test
    void resteDu_deduitLAcompteSurFactureNonPayee() {
        assertThat(StatsService.resteDu(facture(120, 100, false))).isEqualTo(20); // acompte déduit
        assertThat(StatsService.resteDu(facture(50, 0, false))).isEqualTo(50);    // POS : inchangé
        assertThat(StatsService.resteDu(facture(80, 80, false))).isEqualTo(0);    // soldé par l'acompte
        assertThat(StatsService.resteDu(facture(120, 100, true))).isEqualTo(0);   // payée → rien dû
    }

    @Test
    void resteDu_sommeImpayeIgnoreLesAcomptes() {
        long impaye = Stream.of(
                facture(120, 100, false),
                facture(50, 0, false),
                facture(80, 80, false),
                facture(120, 100, true)
        ).mapToLong(StatsService::resteDu).sum();
        assertThat(impaye).isEqualTo(70);
    }
}
