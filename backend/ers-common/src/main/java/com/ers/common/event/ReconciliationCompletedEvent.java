package com.ers.common.event;

import java.util.UUID;

/**
 * Published by the reconciliation module after a run completes, so downstream modules (exception
 * triage) can react without the reconciliation module needing a compile-time dependency on them.
 */
public record ReconciliationCompletedEvent(UUID reconciliationId, String periodCode) {
}
