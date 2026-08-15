package com.ers.adjustment.erp;

import com.ers.adjustment.domain.JournalEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NoOpErpPostingClient implements ErpPostingClient {

    private static final Logger log = LoggerFactory.getLogger(NoOpErpPostingClient.class);

    @Override
    public ErpPostingResult post(JournalEntry journalEntry) {
        String reference = "SIM-" + UUID.randomUUID();
        log.info("[ERP-STUB] Would post journal entry {} ({} {} {}) to ERP as {}",
                journalEntry.getId(), journalEntry.getDebitCredit(), journalEntry.getAmount(),
                journalEntry.getCurrency(), reference);
        return new ErpPostingResult(true, reference, "Simulated posting - no ERP connection configured");
    }
}
