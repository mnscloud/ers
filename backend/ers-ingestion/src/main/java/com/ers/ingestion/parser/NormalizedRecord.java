package com.ers.ingestion.parser;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NormalizedRecord(
        String externalId,
        LocalDate transactionDate,
        BigDecimal amount,
        String currency,
        String description,
        String rawPayloadJson
) {
}
