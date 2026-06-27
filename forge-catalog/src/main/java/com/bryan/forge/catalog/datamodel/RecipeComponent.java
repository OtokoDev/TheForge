package com.bryan.forge.catalog.datamodel;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Une arête du graphe de recettes : l'item {@code outputItem} requiert {@code quantity}
 * unités de {@code componentItem}. Le graphe doit rester un DAG (pas de cycle).
 */
@Entity
@Table(name = "recipe_component",
        uniqueConstraints = @UniqueConstraint(columnNames = {"output_item_id", "component_item_id"}))
public class RecipeComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_item_id", nullable = false)
    private Item outputItem;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "component_item_id", nullable = false)
    private Item componentItem;

    @Column(nullable = false)
    private int quantity;

    protected RecipeComponent() {}

    public RecipeComponent(Item outputItem, Item componentItem, int quantity) {
        this.outputItem = outputItem;
        this.componentItem = componentItem;
        this.quantity = quantity;
    }

    public UUID getId()            { return id; }
    public Item getOutputItem()    { return outputItem; }
    public Item getComponentItem() { return componentItem; }
    public int getQuantity()       { return quantity; }
}
