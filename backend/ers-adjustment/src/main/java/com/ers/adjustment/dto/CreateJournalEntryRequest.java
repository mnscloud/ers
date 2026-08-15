package com.ers.adjustment.dto;

import com.ers.adjustment.domain.DebitCredit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateJournalEntryRequest(
        UUID reconciliationBreakId,
        @NotBlank String accountCode,
        @NotNull DebitCredit debitCredit,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String description,
        @NotBlank String periodCode
) {
}
