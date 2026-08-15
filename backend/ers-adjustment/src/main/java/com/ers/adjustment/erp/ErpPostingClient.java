package com.ers.adjustment.erp;

import com.ers.adjustment.domain.JournalEntry;

/**
 * Integration point for posting approved adjustments back into the core ERP (e.g. Oracle Cloud EPM).
 * No real ERP connection is available in this environment; NoOpErpPostingClient stands in until a
 * real implementation (REST/SOAP client with auth, retries, idempotency key handling) is added.
 */
public interface ErpPostingClient {

    ErpPostingResult post(JournalEntry journalEntry);

    record ErpPostingResult(boolean success, String erpReference, String message) {
    }
}
