package com.ers.reconciliation.dto;

import com.ers.common.enums.ReconciliationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReconciliationTemplateRequest(
        @NotBlank String name,
        @NotNull ReconciliationType type,
        UUID matchRuleId,
        @NotBlank String owner
) {
}
