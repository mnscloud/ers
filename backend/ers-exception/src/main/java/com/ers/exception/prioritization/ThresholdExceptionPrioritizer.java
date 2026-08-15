package com.ers.exception.prioritization;

import com.ers.common.enums.Severity;
import com.ers.ingestion.domain.RawTransactionRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ThresholdExceptionPrioritizer implements ExceptionPrioritizer {

    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("100000");
    private static final BigDecimal HIGH_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal MEDIUM_THRESHOLD = new BigDecimal("1000");

    @Override
    public Severity prioritize(RawTransactionRecord unmatchedRecord) {
        BigDecimal absAmount = unmatchedRecord.getAmount().abs();
        if (absAmount.compareTo(CRITICAL_THRESHOLD) >= 0) {
            return Severity.CRITICAL;
        }
        if (absAmount.compareTo(HIGH_THRESHOLD) >= 0) {
            return Severity.HIGH;
        }
        if (absAmount.compareTo(MEDIUM_THRESHOLD) >= 0) {
            return Severity.MEDIUM;
        }
        return Severity.LOW;
    }
}
