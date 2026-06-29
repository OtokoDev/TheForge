package com.bryan.forge.stats.backend;

import com.bryan.forge.billing.backend.CostingService;
import com.bryan.forge.billing.backend.dto.ItemCostDto;
import com.bryan.forge.billing.datamodel.Facture;
import com.bryan.forge.billing.datamodel.FactureLine;
import com.bryan.forge.billing.datamodel.FactureStatus;
import com.bryan.forge.billing.datamodel.WorkSession;
import com.bryan.forge.billing.datarepository.FactureLineRepository;
import com.bryan.forge.billing.datarepository.FactureRepository;
import com.bryan.forge.billing.datarepository.SessionRepository;
import com.bryan.forge.business.backend.BusinessAccessService;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datamodel.Taxon;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.TaxonRepository;
import com.bryan.forge.core.datamodel.User;
import com.bryan.forge.core.datarepository.UserRepository;
import com.bryan.forge.ledger.backend.LedgerService;
import com.bryan.forge.ledger.backend.dto.StockRowDto;
import com.bryan.forge.ledger.datamodel.Movement;
import com.bryan.forge.ledger.datamodel.MovementType;
import com.bryan.forge.ledger.datarepository.MovementRepository;
import com.bryan.forge.stats.backend.dto.ActivityStatsDto;
import com.bryan.forge.stats.backend.dto.ClientsStatsDto;
import com.bryan.forge.stats.backend.dto.CreancesStatsDto;
import com.bryan.forge.stats.backend.dto.ForgeronsDto;
import com.bryan.forge.stats.backend.dto.NameValue;
import com.bryan.forge.stats.backend.dto.OverviewDto;
import com.bryan.forge.stats.backend.dto.ProductsDto;
import com.bryan.forge.stats.backend.dto.StockStatsDto;
import com.bryan.forge.treasury.backend.CreanceService;
import com.bryan.forge.treasury.backend.dto.CreanceFarmerDto;
import com.bryan.forge.treasury.datamodel.CreanceEntry;
import com.bryan.forge.treasury.datamodel.CreanceType;
import com.bryan.forge.treasury.datarepository.CreanceEntryRepository;
import com.bryan.forge.valuation.datamodel.Product;
import com.bryan.forge.valuation.datarepository.ProductRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Agrégations statistiques (lecture seule) par business sur une période [from, to). */
@Singleton
public class StatsService {

    private static final ZoneId TZ = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(TZ);
    private static final long LOW_STOCK = 5;

    private final FactureRepository factureRepo;
    private final FactureLineRepository lineRepo;
    private final SessionRepository sessionRepo;
    private final MovementRepository movementRepo;
    private final ItemRepository itemRepo;
    private final TaxonRepository taxonRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final LedgerService ledger;
    private final CostingService costing;
    private final BusinessAccessService access;
    private final CreanceEntryRepository creanceRepo;
    private final CreanceService creanceService;

    public StatsService(FactureRepository factureRepo, FactureLineRepository lineRepo, SessionRepository sessionRepo,
                        MovementRepository movementRepo, ItemRepository itemRepo, TaxonRepository taxonRepo,
                        ProductRepository productRepo, UserRepository userRepo, LedgerService ledger,
                        CostingService costing, BusinessAccessService access,
                        CreanceEntryRepository creanceRepo, CreanceService creanceService) {
        this.factureRepo = factureRepo;
        this.lineRepo = lineRepo;
        this.sessionRepo = sessionRepo;
        this.movementRepo = movementRepo;
        this.itemRepo = itemRepo;
        this.taxonRepo = taxonRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.ledger = ledger;
        this.costing = costing;
        this.access = access;
        this.creanceRepo = creanceRepo;
        this.creanceService = creanceService;
    }

    // ── A. Vue d'ensemble ─────────────────────────────────────────────────────
    @Transactional
    public OverviewDto overview(User actor, UUID businessId, Instant from, Instant to) {
        access.requireView(actor, businessId);
        List<Facture> validated = validatedFactures(businessId);
        Instant prevFrom = from.minus(Duration.between(from, to));

        List<Facture> cur = inRange(validated, from, to);
        List<Facture> prev = inRange(validated, prevFrom, from);

        long caEncaisse = cur.stream().filter(Facture::isPaid).mapToLong(Facture::getTotalAmount).sum();
        long caEncaissePrev = prev.stream().filter(Facture::isPaid).mapToLong(Facture::getTotalAmount).sum();
        long caTotal = cur.stream().mapToLong(Facture::getTotalAmount).sum();
        long benefice = cur.stream().mapToLong(f -> lng(f.getTotalProfit())).sum();
        long beneficePrev = prev.stream().mapToLong(f -> lng(f.getTotalProfit())).sum();
        int nb = cur.size();
        int nbPrev = prev.size();
        long panier = nb == 0 ? 0 : caTotal / nb;
        long caTotalPrev = prev.stream().mapToLong(Facture::getTotalAmount).sum();
        long panierPrev = nbPrev == 0 ? 0 : caTotalPrev / nbPrev;

        List<Facture> impayes = cur.stream().filter(f -> !f.isPaid()).toList();
        long impaye = impayes.stream().mapToLong(Facture::getTotalAmount).sum();
        long partBusiness = cur.stream().mapToLong(f -> lng(f.getBusinessShare())).sum();
        long partForgeron = cur.stream().mapToLong(f -> lng(f.getWorkerShare())).sum();

        Map<String, long[]> byDay = new LinkedHashMap<>();   // jour -> [ca, benefice]
        cur.stream().sorted(Comparator.comparing(Facture::getValidatedAt)).forEach(f -> {
            long[] acc = byDay.computeIfAbsent(DAY.format(f.getValidatedAt()), k -> new long[2]);
            acc[0] += f.getTotalAmount();
            acc[1] += lng(f.getTotalProfit());
        });
        List<OverviewDto.DayPoint> serie = byDay.entrySet().stream()
                .map(e -> new OverviewDto.DayPoint(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();

        double tauxMarge = caTotal == 0 ? 0 : (double) benefice / caTotal;
        return new OverviewDto(caEncaisse, caEncaissePrev, benefice, beneficePrev, nb, nbPrev,
                panier, panierPrev, tauxMarge, impaye, impayes.size(), partBusiness, partForgeron, serie);
    }

    // ── B. Produits ───────────────────────────────────────────────────────────
    @Transactional
    public ProductsDto products(User actor, UUID businessId, Instant from, Instant to) {
        access.requireView(actor, businessId);
        Map<UUID, Item> items = itemRepo.findAll().stream().collect(Collectors.toMap(Item::getId, Function.identity()));
        Map<UUID, String> taxa = taxonRepo.findAll().stream().collect(Collectors.toMap(Taxon::getId, Taxon::getNom));

        Map<UUID, long[]> perItem = new HashMap<>();          // itemId -> [ca, marge, qte]
        Map<String, Long> parFamille = new HashMap<>();
        Map<String, Long> parMateriau = new HashMap<>();
        for (Facture f : inRange(validatedFactures(businessId), from, to)) {
            for (FactureLine l : lineRepo.findByFactureId(f.getId())) {
                long ca = l.getLineTotal();
                long marge = l.getUnitPriceSnapshot().subtract(l.getUnitCostSnapshot())
                        .multiply(BigDecimal.valueOf(l.getQuantity())).setScale(0, RoundingMode.HALF_UP).longValue();
                long[] acc = perItem.computeIfAbsent(l.getItemId(), k -> new long[3]);
                acc[0] += ca; acc[1] += marge; acc[2] += l.getQuantity();
                Item it = items.get(l.getItemId());
                parFamille.merge(taxonName(it == null ? null : it.getFamilyId(), taxa), ca, Long::sum);
                parMateriau.merge(taxonName(it == null ? null : it.getMaterialId(), taxa), ca, Long::sum);
            }
        }
        List<ProductsDto.ProductStat> top = perItem.entrySet().stream()
                .map(e -> new ProductsDto.ProductStat(e.getKey().toString(),
                        items.containsKey(e.getKey()) ? items.get(e.getKey()).getName() : "?",
                        e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .sorted(Comparator.comparingLong(ProductsDto.ProductStat::ca).reversed())
                .toList();

        // Alertes « vendu à perte » : produits courants dont le prix de revente < coût de revient.
        Map<UUID, BigDecimal> costs = costing.costs(actor, businessId).stream()
                .collect(Collectors.toMap(ItemCostDto::itemId, ItemCostDto::cost));
        List<ProductsDto.LossAlert> pertes = new ArrayList<>();
        for (Product p : productRepo.findByBusinessIdAndValidToIsNull(businessId)) {
            if (p.getPrixRevente() == null) continue;
            BigDecimal cost = costs.getOrDefault(p.getItemId(), BigDecimal.ZERO);
            if (p.getPrixRevente().compareTo(cost) < 0) {
                Item it = items.get(p.getItemId());
                pertes.add(new ProductsDto.LossAlert(it == null ? "?" : it.getName(),
                        p.getPrixRevente().setScale(0, RoundingMode.HALF_UP).longValue(),
                        cost.setScale(0, RoundingMode.HALF_UP).longValue()));
            }
        }
        return new ProductsDto(top, sortedNameValues(parFamille), sortedNameValues(parMateriau), pertes);
    }

    // ── C. Forgerons ──────────────────────────────────────────────────────────
    @Transactional
    public ForgeronsDto forgerons(User actor, UUID businessId, Instant from, Instant to) {
        access.requireView(actor, businessId);
        Map<UUID, long[]> byUser = new HashMap<>();           // userId -> [ca, benefice, nb, minutes]
        for (Facture f : inRange(validatedFactures(businessId), from, to)) {
            if (f.getCreatedBy() == null) continue;
            long[] acc = byUser.computeIfAbsent(f.getCreatedBy(), k -> new long[4]);
            acc[0] += f.getTotalAmount();
            acc[1] += lng(f.getTotalProfit());
            acc[2] += 1;
        }
        for (WorkSession s : sessionRepo.findByBusinessIdOrderByOpenedAtDesc(businessId)) {
            if (s.getClosedAt() == null || !inRange(s.getClosedAt(), from, to)) continue;
            long[] acc = byUser.computeIfAbsent(s.getUserId(), k -> new long[4]);
            acc[3] += Duration.between(s.getOpenedAt(), s.getClosedAt()).toMinutes();
        }
        Map<UUID, String> names = new HashMap<>();
        List<ForgeronsDto.ForgeronStat> list = byUser.entrySet().stream().map(e -> {
            long[] v = e.getValue();
            double caHeure = v[3] == 0 ? 0 : v[0] / (v[3] / 60.0);
            String name = names.computeIfAbsent(e.getKey(),
                    id -> userRepo.findById(id).map(User::getDisplayName).orElse("?"));
            return new ForgeronsDto.ForgeronStat(e.getKey().toString(), name, v[0], v[1], (int) v[2], v[3], caHeure);
        }).sorted(Comparator.comparingLong(ForgeronsDto.ForgeronStat::ca).reversed()).toList();
        return new ForgeronsDto(list);
    }

    // ── E. Stock ──────────────────────────────────────────────────────────────
    @Transactional
    public StockStatsDto stock(User actor, UUID businessId, Instant from, Instant to) {
        access.requireView(actor, businessId);
        Map<UUID, BigDecimal> costs = costing.costs(actor, businessId).stream()
                .collect(Collectors.toMap(ItemCostDto::itemId, ItemCostDto::cost));

        Map<UUID, String> itemNames = itemRepo.findAll().stream()
                .collect(Collectors.toMap(Item::getId, Item::getName));
        Map<UUID, Long> qtyByItem = new HashMap<>();          // itemId -> qty (solde agrégé)
        for (StockRowDto r : ledger.stock(actor, businessId)) {
            qtyByItem.merge(r.itemId(), (long) r.quantity(), Long::sum);
        }
        long valeurStock = 0;
        for (Map.Entry<UUID, Long> e : qtyByItem.entrySet()) {
            valeurStock += costs.getOrDefault(e.getKey(), BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(e.getValue())).setScale(0, RoundingMode.HALF_UP).longValue();
        }
        // Ruptures = produits VENDABLES (prix de revente défini) dont le stock est faible/0.
        Set<UUID> sellable = productRepo.findByBusinessIdAndValidToIsNull(businessId).stream()
                .filter(p -> p.getPrixRevente() != null).map(Product::getItemId).collect(Collectors.toSet());
        List<NameValue> ruptures = new ArrayList<>();
        for (UUID id : sellable) {
            long qty = qtyByItem.getOrDefault(id, 0L);
            if (qty <= LOW_STOCK) ruptures.add(new NameValue(itemNames.getOrDefault(id, "?"), qty));
        }
        ruptures.sort(Comparator.comparingLong(NameValue::valeur));

        // Top matières consommées : sorties (SALE / WITHDRAWAL) sur la période.
        Map<UUID, Long> outByItem = new HashMap<>();
        for (Movement m : movementRepo.findByBusinessIdOrderByCreatedAtDesc(businessId)) {
            if (m.getFromAccountId() == null || !inRange(m.getCreatedAt(), from, to)) continue;
            if (m.getType() == MovementType.SALE || m.getType() == MovementType.WITHDRAWAL) {
                outByItem.merge(m.getItemId(), (long) m.getQuantity(), Long::sum);
            }
        }
        List<NameValue> topConsommees = outByItem.entrySet().stream()
                .map(e -> new NameValue(itemNames.getOrDefault(e.getKey(), "?"), e.getValue()))
                .sorted(Comparator.comparingLong(NameValue::valeur).reversed())
                .limit(10).toList();

        return new StockStatsDto(valeurStock, ruptures, topConsommees);
    }

    // ── D. Activité ───────────────────────────────────────────────────────────
    @Transactional
    public ActivityStatsDto activity(User actor, UUID businessId, Instant from, Instant to) {
        access.requireView(actor, businessId);
        Map<Long, long[]> heat = new HashMap<>();             // dow*24+hour -> [ca, count]
        for (Facture f : inRange(validatedFactures(businessId), from, to)) {
            LocalDateTime ldt = LocalDateTime.ofInstant(f.getValidatedAt(), TZ);
            long key = (ldt.getDayOfWeek().getValue() - 1) * 24L + ldt.getHour();
            long[] acc = heat.computeIfAbsent(key, k -> new long[2]);
            acc[0] += f.getTotalAmount();
            acc[1] += 1;
        }
        List<ActivityStatsDto.HeatCell> cells = heat.entrySet().stream()
                .map(e -> new ActivityStatsDto.HeatCell((int) (e.getKey() / 24), (int) (e.getKey() % 24),
                        e.getValue()[0], (int) e.getValue()[1])).toList();

        List<WorkSession> sess = sessionRepo.findByBusinessIdOrderByOpenedAtDesc(businessId).stream()
                .filter(s -> s.getClosedAt() != null && inRange(s.getClosedAt(), from, to)).toList();
        int n = sess.size();
        long avgMin = n == 0 ? 0 : sess.stream()
                .mapToLong(s -> Duration.between(s.getOpenedAt(), s.getClosedAt()).toMinutes()).sum() / n;
        long caParSession = n == 0 ? 0 : sess.stream().mapToLong(WorkSession::getTotalSales).sum() / n;
        return new ActivityStatsDto(cells, n, avgMin, caParSession);
    }

    // ── F. Créances ───────────────────────────────────────────────────────────
    @Transactional
    public CreancesStatsDto creances(User actor, UUID businessId, Instant from, Instant to) {
        access.requireView(actor, businessId);
        List<CreanceFarmerDto> farmers = creanceService.listFarmers(actor, businessId);
        long totalDu = farmers.stream().mapToLong(CreanceFarmerDto::remaining).sum();
        long totalCredit = farmers.stream().mapToLong(CreanceFarmerDto::totalCredit).sum();
        long totalPaid = farmers.stream().mapToLong(CreanceFarmerDto::totalPaid).sum();
        double ratio = totalCredit == 0 ? 0 : (double) totalPaid / totalCredit;

        Map<String, long[]> byDay = new LinkedHashMap<>();    // jour -> [credit, paiement]
        creanceRepo.findByBusinessId(businessId).stream()
                .filter(e -> inRange(e.getCreatedAt(), from, to))
                .sorted(Comparator.comparing(CreanceEntry::getCreatedAt))
                .forEach(e -> {
                    long[] acc = byDay.computeIfAbsent(DAY.format(e.getCreatedAt()), k -> new long[2]);
                    if (e.getType() == CreanceType.CREDIT) acc[0] += e.getAmount(); else acc[1] += e.getAmount();
                });
        List<CreancesStatsDto.DayCreance> serie = byDay.entrySet().stream()
                .map(e -> new CreancesStatsDto.DayCreance(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
        List<CreancesStatsDto.FarmerStat> top = farmers.stream()
                .sorted(Comparator.comparingLong(CreanceFarmerDto::remaining).reversed()).limit(10)
                .map(f -> new CreancesStatsDto.FarmerStat(
                        f.farmerInGameName() != null && !f.farmerInGameName().isBlank() ? f.farmerInGameName() : f.farmerUsername(),
                        f.totalCredit(), f.totalPaid(), f.remaining()))
                .toList();
        return new CreancesStatsDto(totalDu, totalCredit, totalPaid, ratio, serie, top);
    }

    // ── G. Clients ────────────────────────────────────────────────────────────
    @Transactional
    public ClientsStatsDto clients(User actor, UUID businessId, Instant from, Instant to) {
        access.requireView(actor, businessId);
        Map<String, long[]> byClient = new HashMap<>();       // nom -> [ca, nb, impaye]
        for (Facture f : inRange(validatedFactures(businessId), from, to)) {
            String nom = f.getClientName() == null || f.getClientName().isBlank() ? "Client de passage" : f.getClientName();
            long[] acc = byClient.computeIfAbsent(nom, k -> new long[3]);
            acc[0] += f.getTotalAmount();
            acc[1] += 1;
            if (!f.isPaid()) acc[2] += f.getTotalAmount();
        }
        List<ClientsStatsDto.ClientStat> all = byClient.entrySet().stream()
                .map(e -> new ClientsStatsDto.ClientStat(e.getKey(), e.getValue()[0], (int) e.getValue()[1], e.getValue()[2]))
                .toList();
        List<ClientsStatsDto.ClientStat> top = all.stream()
                .sorted(Comparator.comparingLong(ClientsStatsDto.ClientStat::ca).reversed()).limit(10).toList();
        List<ClientsStatsDto.ClientStat> debiteurs = all.stream().filter(c -> c.impaye() > 0)
                .sorted(Comparator.comparingLong(ClientsStatsDto.ClientStat::impaye).reversed()).toList();
        return new ClientsStatsDto(top, debiteurs);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private List<Facture> validatedFactures(UUID businessId) {
        return factureRepo.findByBusinessIdOrderByNumeroDesc(businessId).stream()
                .filter(f -> f.getStatus() == FactureStatus.VALIDEE && f.getValidatedAt() != null)
                .toList();
    }

    private static List<Facture> inRange(List<Facture> factures, Instant from, Instant to) {
        return factures.stream().filter(f -> inRange(f.getValidatedAt(), from, to)).toList();
    }

    private static boolean inRange(Instant t, Instant from, Instant to) {
        return t != null && !t.isBefore(from) && t.isBefore(to);
    }

    private static long lng(BigDecimal b) {
        return b == null ? 0 : b.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static String taxonName(UUID id, Map<UUID, String> taxa) {
        return id == null ? "Sans" : taxa.getOrDefault(id, "?");
    }

    private static List<NameValue> sortedNameValues(Map<String, Long> map) {
        return map.entrySet().stream()
                .map(e -> new NameValue(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(NameValue::valeur).reversed())
                .toList();
    }
}
