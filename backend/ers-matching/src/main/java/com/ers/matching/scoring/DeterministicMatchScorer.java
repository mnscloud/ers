package com.ers.matching.scoring;

import com.ers.ingestion.domain.RawTransactionRecord;
import com.ers.matching.domain.MatchRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

@Component
public class DeterministicMatchScorer implements MatchScorer {

    @Override
    public double score(RawTransactionRecord a, RawTransactionRecord b, MatchRule rule) {
        if (!a.getCurrency().equalsIgnoreCase(b.getCurrency())) {
            return 0.0;
        }

        BigDecimal amountDiff = a.getAmount().abs().subtract(b.getAmount().abs()).abs();
        if (amountDiff.compareTo(rule.getAmountTolerance()) > 0) {
            return 0.0;
        }

        long daysApart = Math.abs(ChronoUnit.DAYS.between(a.getTransactionDate(), b.getTransactionDate()));
        if (daysApart > rule.getDateToleranceDays()) {
            return 0.0;
        }

        if (a.getExternalId().equalsIgnoreCase(b.getExternalId())) {
            return 1.0;
        }

        double amountScore = rule.getAmountTolerance().signum() == 0
                ? 1.0
                : 1.0 - (amountDiff.doubleValue() / Math.max(rule.getAmountTolerance().doubleValue(), 0.0001));
        double dateScore = rule.getDateToleranceDays() == 0
                ? 1.0
                : 1.0 - ((double) daysApart / Math.max(rule.getDateToleranceDays(), 1));

        return Math.max(0.0, Math.min(1.0, (amountScore + dateScore) / 2.0));
    }
}
