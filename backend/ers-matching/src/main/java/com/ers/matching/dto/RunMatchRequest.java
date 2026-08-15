package com.ers.matching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RunMatchRequest(
        @NotNull UUID matchRuleId,
        @NotBlank String periodCode
) {
}
