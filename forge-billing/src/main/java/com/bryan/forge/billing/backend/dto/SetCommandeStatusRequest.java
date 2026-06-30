package com.bryan.forge.billing.backend.dto;

import com.bryan.forge.billing.datamodel.CommandeStatus;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record SetCommandeStatusRequest(CommandeStatus status) {}
