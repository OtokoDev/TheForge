package com.bryan.forge.catalog.backend;

import com.bryan.forge.catalog.backend.dto.TaxonDto;
import com.bryan.forge.catalog.datamodel.Taxon;
import com.bryan.forge.catalog.datamodel.TaxonKind;
import com.bryan.forge.catalog.datarepository.TaxonRepository;
import com.bryan.forge.security.backend.CurrentActor;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gère les listes de classement (familles, matériaux). Lecture libre (authentifié) ;
 * l'écriture est réservée à SYSTEM par les contrôleurs (@Secured("ROLE_SYSTEM")).
 */
@Singleton
public class TaxonService {

    private final TaxonRepository repo;
    private final CurrentActor currentActor;

    public TaxonService(TaxonRepository repo, CurrentActor currentActor) {
        this.repo = repo;
        this.currentActor = currentActor;
    }

    @Transactional
    public List<TaxonDto> list(TaxonKind kind) {
        return repo.findByKind(kind).stream()
                .sorted(Comparator.comparingInt(Taxon::getOrdre).thenComparing(Taxon::getNom, String.CASE_INSENSITIVE_ORDER))
                .map(TaxonDto::from)
                .toList();
    }

    @Transactional
    public TaxonDto create(TaxonKind kind, String nom, String couleur) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire");
        }
        int nextOrdre = repo.findByKind(kind).stream().mapToInt(Taxon::getOrdre).max().orElse(-1) + 1;
        Taxon taxon = new Taxon(kind, nom.trim(), nextOrdre, blank(couleur));
        UUID by = currentActor.stampId();
        taxon.setCreatedBy(by);
        taxon.setModifiedBy(by);
        return TaxonDto.from(repo.save(taxon));
    }

    @Transactional
    public TaxonDto update(TaxonKind kind, UUID id, String nom, int ordre, String couleur, int version) {
        Taxon t = require(kind, id);
        com.bryan.forge.core.backend.StaleDataException.check(t.getVersion(), version);
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire");
        }
        t.setNom(nom.trim());
        t.setOrdre(ordre);
        t.setCouleur(blank(couleur));
        t.setModifiedBy(currentActor.stampId());
        return TaxonDto.from(repo.update(t));
    }

    @Transactional
    public void delete(TaxonKind kind, UUID id) {
        repo.delete(require(kind, id));
    }

    /** Réordonne : {@code ordre} = position dans la liste fournie. */
    @Transactional
    public List<TaxonDto> reorder(TaxonKind kind, List<UUID> orderedIds) {
        java.util.Map<UUID, Taxon> byId = repo.findByKind(kind).stream()
                .collect(Collectors.toMap(Taxon::getId, t -> t));
        int i = 0;
        for (UUID id : orderedIds) {
            Taxon t = byId.get(id);
            if (t != null) {
                t.setOrdre(i++);
                repo.update(t);
            }
        }
        return list(kind);
    }

    private Taxon require(TaxonKind kind, UUID id) {
        Taxon t = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Élément introuvable : " + id));
        if (t.getKind() != kind) {
            throw new NoSuchElementException("Élément introuvable : " + id);
        }
        return t;
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
