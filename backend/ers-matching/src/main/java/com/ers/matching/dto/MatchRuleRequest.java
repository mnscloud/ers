package com.ers.matching.dto;

import com.ers.common.enums.MatchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record MatchRuleRequest(
        @NotBlank String name,
        @NotBlank String sourceSystemA,
        @NotBlank String sourceSystemB,
        @NotNull MatchType matchType,
        @NotNull @PositiveOrZero BigDecimal amountTolerance,
        @PositiveOrZero int dateToleranceDays
) {
}
