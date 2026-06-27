package com.bryan.forge;

import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datamodel.BusinessType;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.datamodel.Account;
import com.bryan.forge.ledger.datamodel.AccountKind;
import com.bryan.forge.ledger.datamodel.MovementType;
import com.bryan.forge.ledger.datarepository.AccountRepository;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garantit le suivi de stock sous concurrence : N retraits simultanés sur un stock de S
 * (N > S) ne doivent JAMAIS produire d'oversell. Le verrou pessimiste ({@code FOR UPDATE}
 * sur le compte) sérialise la garde « stock négatif » → exactement S retraits réussissent.
 *
 * Intégration (Postgres via Testcontainers) — hors build par défaut.
 * Lancer : {@code mvn test -Dgroups=integration} (Docker requis).
 */
@MicronautTest(transactional = false)
@Property(name = "micronaut.security.oauth2.clients.discord.client-id", value = "test")
@Property(name = "micronaut.security.oauth2.clients.discord.client-secret", value = "test")
@Tag("integration")
class StockConcurrencyTest {

    @Inject
    LedgerService ledger;
    @Inject
    BusinessRepository businessRepo;
    @Inject
    ItemRepository itemRepo;
    @Inject
    AccountRepository accountRepo;

    @Test
    void gardeStockNegatifSousConcurrence() throws Exception {
        UUID biz = businessRepo.save(new Business("ForgeTest-" + UUID.randomUUID(), BusinessType.FORGE)).getId();
        UUID itemId = itemRepo.save(new Item("Lingot test", null, null, null)).getId();
        UUID account = accountRepo.save(new Account(biz, "Stock", AccountKind.STOCK)).getId();

        int stock = 50;
        ledger.applyMovement(biz, itemId, stock, null, account, MovementType.DEPOSIT, "SEED", null, "seed", null);

        int threads = 80;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    ledger.applyMovement(biz, itemId, 1, account, null, MovementType.WITHDRAWAL, "T", null, null, null);
                    ok.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    refused.incrementAndGet();   // « stock insuffisant » (ou conflit de verrou)
                }
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(ok.get()).as("retraits réussis").isEqualTo(stock);
        assertThat(refused.get()).as("retraits refusés").isEqualTo(threads - stock);
    }
}
