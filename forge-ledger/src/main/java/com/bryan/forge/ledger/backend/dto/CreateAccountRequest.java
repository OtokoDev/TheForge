package com.bryan.forge.ledger.backend.dto;

import com.bryan.forge.ledger.datamodel.AccountKind;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record CreateAccountRequest(String name, AccountKind kind) {}
