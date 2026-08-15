package com.ers.matching.scoring;

import com.ers.ingestion.domain.RawTransactionRecord;
import com.ers.matching.domain.MatchRule;

/**
 * Extension point for the matching algorithm. The default implementation is deterministic
 * (exact + tolerance-based amount/date comparison). A future ML-based scorer can implement
 * this same interface (e.g. returning a learned similarity score) and be swapped in via
 * Spring qualifier/profile without changing MatchingEngine.
 */
public interface MatchScorer {

    /**
     * @return a confidence score in [0.0, 1.0]; the engine treats scores below its threshold as non-matches.
     */
    double score(RawTransactionRecord a, RawTransactionRecord b, MatchRule rule);
}
