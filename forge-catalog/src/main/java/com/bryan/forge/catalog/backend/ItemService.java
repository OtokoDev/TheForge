package com.bryan.forge.catalog.backend;

import com.bryan.forge.catalog.backend.dto.CreateItemRequest;
import com.bryan.forge.catalog.backend.dto.ItemDto;
import com.bryan.forge.catalog.backend.dto.UpdateItemRequest;
import com.bryan.forge.catalog.datamodel.Item;
import com.bryan.forge.catalog.datamodel.Taxon;
import com.bryan.forge.catalog.datamodel.TaxonKind;
import com.bryan.forge.catalog.datarepository.ItemRepository;
import com.bryan.forge.catalog.datarepository.RecipeComponentRepository;
import com.bryan.forge.catalog.datarepository.TaxonRepository;
import com.bryan.forge.core.backend.StaleDataException;
import com.bryan.forge.security.backend.CurrentActor;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ItemService {

    private final ItemRepository repo;
    private final RecipeComponentRepository recipeRepo;
    private final TaxonRepository taxonRepo;
    private final CurrentActor currentActor;

    public ItemService(ItemRepository repo, RecipeComponentRepository recipeRepo, TaxonRepository taxonRepo,
                       CurrentActor currentActor) {
        this.repo = repo;
        this.recipeRepo = recipeRepo;
        this.taxonRepo = taxonRepo;
        this.currentActor = currentActor;
    }

    @Transactional
    public List<ItemDto> listAll() {
        Set<UUID> withRecipe = recipeRepo.findAll().stream()
                .map(rc -> rc.getOutputItem().getId())
                .collect(Collectors.toSet());
        Map<UUID, Taxon> taxa = taxonRepo.findAll().stream()
                .collect(Collectors.toMap(Taxon::getId, Function.identity()));
        return repo.findAll().stream()
                .sorted(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER))
                .map(i -> toDto(i, withRecipe.contains(i.getId()), taxa.get(i.getFamilyId()), taxa.get(i.getMaterialId())))
                .toList();
    }

    @Transactional
    public ItemDto create(CreateItemRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("Le nom de l'item est obligatoire");
        }
        requireKind(req.familyId(), TaxonKind.FAMILY);
        requireKind(req.materialId(), TaxonKind.MATERIAL);
        Item item = new Item(req.name().trim(), req.familyId(), req.materialId(), req.handRequired());
        UUID by = currentActor.stampId();
        item.setCreatedBy(by);
        item.setModifiedBy(by);
        Item saved = repo.save(item);
        return toDto(saved, false, taxon(saved.getFamilyId()), taxon(saved.getMaterialId()));
    }

    @Transactional
    public ItemDto update(UUID id, UpdateItemRequest req) {
        Item item = require(id);
        StaleDataException.check(item.getVersion(), req.version());
        if (item.isSystem() && !req.active()) {
            throw new IllegalStateException("L'item système ne peut pas être désactivé");
        }
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("Le nom de l'item est obligatoire");
        }
        requireKind(req.familyId(), TaxonKind.FAMILY);
        requireKind(req.materialId(), TaxonKind.MATERIAL);
        item.setName(req.name().trim());
        item.setFamilyId(req.familyId());
        item.setMaterialId(req.materialId());
        item.setHandRequired(req.handRequired());
        item.setActive(req.active());
        item.setModifiedBy(currentActor.stampId());
        Item updated = repo.update(item);
        return toDto(updated, recipeRepo.existsByOutputItemId(id), taxon(updated.getFamilyId()), taxon(updated.getMaterialId()));
    }

    @Transactional
    public void delete(UUID id) {
        Item item = require(id);
        if (item.isSystem()) {
            throw new IllegalStateException("L'item système ne peut pas être supprimé");
        }
        if (recipeRepo.existsByComponentItemId(id)) {
            throw new IllegalStateException("Item utilisé comme composant dans une recette");
        }
        // Les recettes dont cet item est le résultat sont supprimées en cascade (FK).
        repo.delete(item);
    }

    private ItemDto toDto(Item i, boolean hasRecipe, Taxon family, Taxon material) {
        return new ItemDto(i.getId(), i.getName(),
                i.getFamilyId(), family == null ? null : family.getNom(), family == null ? null : family.getCouleur(),
                i.getMaterialId(), material == null ? null : material.getNom(), material == null ? null : material.getCouleur(),
                i.getHandRequired(), i.isActive(), i.isSystem(), hasRecipe, i.getVersion());
    }

    private void requireKind(UUID id, TaxonKind kind) {
        if (id != null && !taxonRepo.existsByIdAndKind(id, kind)) {
            throw new IllegalArgumentException((kind == TaxonKind.FAMILY ? "Famille" : "Matériau") + " inconnu : " + id);
        }
    }

    private Taxon taxon(UUID id) {
        return id == null ? null : taxonRepo.findById(id).orElse(null);
    }

    private Item require(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item introuvable : " + id));
    }
}
