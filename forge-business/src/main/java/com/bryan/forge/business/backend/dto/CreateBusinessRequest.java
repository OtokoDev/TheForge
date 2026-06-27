package com.bryan.forge.business.backend.dto;

import com.bryan.forge.business.datamodel.BusinessType;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record CreateBusinessRequest(String nom, BusinessType type) {}
