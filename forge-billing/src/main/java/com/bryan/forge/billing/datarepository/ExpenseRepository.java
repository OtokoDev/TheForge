package com.bryan.forge.billing.datarepository;

import com.bryan.forge.billing.datamodel.Expense;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);
}
