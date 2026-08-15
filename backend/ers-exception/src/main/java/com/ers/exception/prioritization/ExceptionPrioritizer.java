package com.ers.exception.prioritization;

import com.ers.common.enums.Severity;
import com.ers.ingestion.domain.RawTransactionRecord;

/**
 * Extension point for break triage. The default implementation applies fixed amount thresholds;
 * swap in an ML-based implementation of this interface (e.g. trained on historical resolution time
 * and false-escalation rate) without touching BreakDetectionListener or the triage workflow.
 */
public interface ExceptionPrioritizer {

    Severity prioritize(RawTransactionRecord unmatchedRecord);
}
