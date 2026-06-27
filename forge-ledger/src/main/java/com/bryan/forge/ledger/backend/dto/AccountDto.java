package com.bryan.forge.ledger.backend.dto;

import com.bryan.forge.ledger.datamodel.Account;
import com.bryan.forge.ledger.datamodel.AccountKind;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record AccountDto(UUID id, String name, AccountKind kind) {
    public static AccountDto from(Account a) {
        return new AccountDto(a.getId(), a.getName(), a.getKind());
    }
}
