package com.bryan.forge.ledger.datamodel;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Solde matérialisé d'un item sur un compte (qty courante). Maintenu à chaque mouvement
 * (sous le verrou du compte) — évite de resommer tout l'historique à chaque lecture.
 */
@Entity
@Table(name = "stock_balance", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "item_id"}))
public class StockBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", columnDefinition = "uuid", nullable = false)
    private UUID accountId;

    @Column(name = "item_id", columnDefinition = "uuid", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private long qty;

    protected StockBalance() {}

    public StockBalance(UUID accountId, UUID itemId, long qty) {
        this.accountId = accountId;
        this.itemId = itemId;
        this.qty = qty;
    }

    public UUID getId()        { return id; }
    public UUID getAccountId() { return accountId; }
    public UUID getItemId()    { return itemId; }
    public long getQty()       { return qty; }
    public void setQty(long qty) { this.qty = qty; }
}
