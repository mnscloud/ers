package com.ers.reconciliation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateReconciliationRequest(
        @NotNull UUID templateId,
        @NotBlank String periodCode
) {
}
