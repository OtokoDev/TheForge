package com.bryan.forge.stats.backend;

import com.bryan.forge.billing.datamodel.Facture;
import com.bryan.forge.billing.datamodel.FactureLine;
import com.bryan.forge.billing.datamodel.FactureStatus;
import com.bryan.forge.billing.datarepository.FactureLineRepository;
import com.bryan.forge.billing.datarepository.FactureRepository;
import com.bryan.forge.business.datamodel.Business;
import com.bryan.forge.business.datarepository.BusinessRepository;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.stats.backend.dto.GlobalStatsDto;
import com.bryan.forge.stats.backend.dto.NameValue;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Statistiques GLOBALES (tous business), réservées aux rôles globaux STAFF/SYSTEM
 * (lecture seule, pas de scope business). Cf. CDC §5 : STAFF = supervision lecture.
 */
@Singleton
public class GlobalStatsService {

    private static final ZoneId TZ = ZoneId.of("Europe/Paris");

    private final FactureRepository factureRepo;
    private final FactureLineRepository lineRepo;
    private final BusinessRepository businessRepo;
    private final ItemRepository itemRepo;

    public GlobalStatsService(FactureRepository factureRepo, FactureLineRepository lineRepo,
                              BusinessRepository businessRepo, ItemRepository itemRepo) {
        this.factureRepo = factureRepo;
        this.lineRepo = lineRepo;
        this.businessRepo = businessRepo;
        this.itemRepo = itemRepo;
    }

    @Transactional
    public GlobalStatsDto overview(Instant from, Instant to) {
        List<Facture> cur = factureRepo.findAll().stream()
                .filter(f -> f.getStatus() == FactureStatus.VALIDEE && inRange(f.getValidatedAt(), from, to))
                .toList();

        long totalCa = cur.stream().mapToLong(Facture::getTotalAmount).sum();
        long totalBenefice = cur.stream().mapToLong(f -> lng(f.getTotalProfit())).sum();

        // CA par semaine ISO.
        Map<String, long[]> byWeek = new LinkedHashMap<>();   // "YYYY-Www" -> [ca, benefice]
        cur.stream().sorted(Comparator.comparing(Facture::getValidatedAt)).forEach(f -> {
            long[] acc = byWeek.computeIfAbsent(week(f.getValidatedAt()), k -> new long[2]);
            acc[0] += f.getTotalAmount();
            acc[1] += lng(f.getTotalProfit());
        });
        List<GlobalStatsDto.WeekPoint> serie = byWeek.entrySet().stream()
                .map(e -> new GlobalStatsDto.WeekPoint(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();

        // Ventilation par business.
        Map<UUID, String> bizNames = businessRepo.findAll().stream()
                .collect(Collectors.toMap(Business::getId, Business::getNom));
        Map<String, Long> parBusiness = new HashMap<>();
        for (Facture f : cur) {
            parBusiness.merge(bizNames.getOrDefault(f.getBusinessId(), "?"), f.getTotalAmount(), Long::sum);
        }

        // Items les plus vendus (toutes forges).
        Map<UUID, String> itemNames = itemRepo.findAll().stream().collect(Collectors.toMap(Item::getId, Item::getName));
        Map<UUID, Long> qtyByItem = new HashMap<>();
        for (Facture f : cur) {
            for (FactureLine l : lineRepo.findByFactureId(f.getId())) {
                qtyByItem.merge(l.getItemId(), (long) l.getQuantity(), Long::sum);
            }
        }
        List<NameValue> topItems = qtyByItem.entrySet().stream()
                .map(e -> new NameValue(itemNames.getOrDefault(e.getKey(), "?"), e.getValue()))
                .sorted(Comparator.comparingLong(NameValue::valeur).reversed()).limit(15).toList();

        return new GlobalStatsDto(totalCa, totalBenefice, serie, sorted(parBusiness), topItems);
    }

    private static String week(Instant t) {
        LocalDate d = LocalDate.ofInstant(t, TZ);
        return d.get(IsoFields.WEEK_BASED_YEAR) + "-W" + String.format("%02d", d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    private static boolean inRange(Instant t, Instant from, Instant to) {
        return t != null && !t.isBefore(from) && t.isBefore(to);
    }

    private static long lng(BigDecimal b) {
        return b == null ? 0 : b.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static List<NameValue> sorted(Map<String, Long> map) {
        return map.entrySet().stream().map(e -> new NameValue(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(NameValue::valeur).reversed()).toList();
    }
}
