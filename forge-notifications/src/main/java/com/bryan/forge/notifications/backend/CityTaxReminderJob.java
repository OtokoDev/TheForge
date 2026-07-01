package com.bryan.forge.notifications.backend;

import com.bryan.forge.billing.backend.FinanceService;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Rappel hebdomadaire de la taxe de la ville : chaque lundi 09:00, un webhook est posté
 * pour chaque business dont la taxe due est > 0 (canal du business, sinon fallback global).
 */
@Singleton
public class CityTaxReminderJob {

    private static final Logger LOG = LoggerFactory.getLogger(CityTaxReminderJob.class);
    private static final int AMBER = 15105570;

    private final BusinessRepository businessRepo;
    private final FinanceService financeService;
    private final WebhookService webhooks;
    private final String fallbackUrl;

    public CityTaxReminderJob(BusinessRepository businessRepo, FinanceService financeService,
                              WebhookService webhooks,
                              @Value("${DISCORD_WEBHOOK_SHOP:}") String fallbackUrl) {
        this.businessRepo = businessRepo;
        this.financeService = financeService;
        this.webhooks = webhooks;
        this.fallbackUrl = fallbackUrl;
    }

    @Scheduled(cron = "0 0 9 * * MON")
    void remind() {
        for (Business b : businessRepo.findAll()) {
            try {
                long due = financeService.cityTaxDueInternal(b.getId());
                if (due <= 0) continue;
                String url = b.getWebhookUrl() != null && !b.getWebhookUrl().isBlank()
                        ? b.getWebhookUrl() : fallbackUrl;
                webhooks.send(url, "TAX_REMINDER", Map.of("embeds", List.of(Map.of(
                        "title", "Taxe de la ville — rappel hebdomadaire",
                        "color", AMBER,
                        "description", "**" + b.getNom() + "** doit **" + due
                                + " septims** à la ville. À reverser dans Finance → Taxe.",
                        "footer", Map.of("text", "Forge RP"),
                        "timestamp", Instant.now().toString()))));
            } catch (Exception e) {
                LOG.warn("Rappel taxe échoué pour {} : {}", b.getNom(), e.getMessage());
            }
        }
    }
}
