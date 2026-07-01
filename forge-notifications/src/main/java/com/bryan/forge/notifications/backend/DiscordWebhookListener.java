package com.bryan.forge.notifications.backend;

import com.bryan.forge.billing.event.FactureValidatedEvent;
import com.bryan.forge.billing.event.ShiftClosedEvent;
import com.bryan.forge.billing.event.ShiftOpenedEvent;
import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Écoute les événements de domaine et émet les webhooks Discord (async, hors transaction).
 * URLs en dur via variables d'env. Respecte le toggle par utilisateur (actorWebhooksEnabled).
 */
@Singleton
public class DiscordWebhookListener {

    private static final int GREEN = 5763719;
    private static final int RED = 15548997;
    private static final DateTimeFormatter HOUR =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Europe/Paris"));

    private final String shopUrl;
    private final String ordersUrl;
    private final WebhookService webhooks;

    public DiscordWebhookListener(@Value("${DISCORD_WEBHOOK_SHOP:}") String shopUrl,
                                  @Value("${DISCORD_WEBHOOK_ORDERS:}") String ordersUrl,
                                  WebhookService webhooks) {
        this.shopUrl = shopUrl;
        this.ordersUrl = ordersUrl;
        this.webhooks = webhooks;
    }

    /** URL du business si configurée, sinon fallback global (env). */
    private static String resolve(String businessUrl, String fallback) {
        return businessUrl != null && !businessUrl.isBlank() ? businessUrl : fallback;
    }

    @EventListener
    @Async
    void onShiftOpened(ShiftOpenedEvent e) {
        if (!e.actorWebhooksEnabled()) return;
        webhooks.send(resolve(e.webhookUrl(), shopUrl), "SHIFT_OPEN", embed("Prise de service", GREEN, List.of(
                field("Forgeron", e.actorUsername()),
                field("Business", e.businessName()),
                field("Heure", HOUR.format(e.openedAt())))));
    }

    @EventListener
    @Async
    void onFactureValidated(FactureValidatedEvent e) {
        if (!e.actorWebhooksEnabled()) return;
        // Chiffres entiers (septim insécable) et cohérents : bénéfice = total − coût ;
        // part forgeron = bénéfice − part business.
        long cost = round(e.totalCost());
        long profit = e.totalAmount() - cost;
        long business = round(e.businessShare());
        webhooks.send(resolve(e.webhookUrl(), ordersUrl), "FACTURE", embed("Facture #" + pad(e.numero()) + " validée", GREEN, List.of(
                field("Forgeron", e.actorUsername()),
                field("Total", money(e.totalAmount())),
                field("Coût de revient", money(cost)),
                field("Bénéfice", money(profit)),
                field("Part business", money(business)),
                field("Part forgeron", money(profit - business)))));
    }

    @EventListener
    @Async
    void onShiftClosed(ShiftClosedEvent e) {
        if (!e.actorWebhooksEnabled()) return;
        long profit = round(e.totalProfit());
        long business = round(e.businessShare());
        webhooks.send(resolve(e.webhookUrl(), shopUrl), "SHIFT_CLOSE", embed("Fin de service", RED, List.of(
                field("Forgeron", e.actorUsername()),
                field("Durée", duration(e.openedAt(), e.closedAt())),
                field("Factures", String.valueOf(e.ordersCount())),
                field("CA", money(e.totalSales())),
                field("Bénéfice", money(profit)),
                field("Part business", money(business)),
                field("Part forgeron", money(profit - business)))));
    }

    // ── Helpers embed ───────────────────────────────────────────────────────

    private static Map<String, Object> embed(String title, int color, List<Map<String, Object>> fields) {
        return Map.of("embeds", List.of(Map.of(
                "title", title,
                "color", color,
                "fields", fields,
                "footer", Map.of("text", "Forge RP"),
                "timestamp", Instant.now().toString())));
    }

    private static Map<String, Object> field(String name, String value) {
        return Map.of("name", name, "value", value.isEmpty() ? "—" : value, "inline", true);
    }

    private static String money(long amount) {
        return amount + " septims";
    }

    /** Arrondi entier (septim insécable), HALF_UP. */
    private static long round(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static String pad(long numero) {
        return String.format("%04d", numero);
    }

    private static String duration(Instant from, Instant to) {
        long minutes = Duration.between(from, to).toMinutes();
        return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "min" : minutes + "min";
    }
}
