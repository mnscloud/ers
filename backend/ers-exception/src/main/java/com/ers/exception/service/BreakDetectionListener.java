package com.ers.exception.service;

import com.ers.common.enums.Severity;
import com.ers.common.event.ReconciliationCompletedEvent;
import com.ers.exception.domain.BreakCategory;
import com.ers.exception.domain.BreakStatus;
import com.ers.exception.domain.ReconciliationBreak;
import com.ers.exception.prioritization.ExceptionPrioritizer;
import com.ers.exception.repository.ReconciliationBreakRepository;
import com.ers.ingestion.domain.RawTransactionRecord;
import com.ers.ingestion.domain.RecordMatchStatus;
import com.ers.ingestion.repository.RawTransactionRecordRepository;
import com.ers.reconciliation.domain.Reconciliation;
import com.ers.reconciliation.domain.ReconciliationTemplate;
import com.ers.reconciliation.repository.ReconciliationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Component
public class BreakDetectionListener {

    private final ReconciliationRepository reconciliationRepository;
    private final RawTransactionRecordRepository recordRepository;
    private final ReconciliationBreakRepository breakRepository;
    private final ExceptionPrioritizer exceptionPrioritizer;

    public BreakDetectionListener(ReconciliationRepository reconciliationRepository,
                                   RawTransactionRecordRepository recordRepository,
                                   ReconciliationBreakRepository breakRepository,
                                   ExceptionPrioritizer exceptionPrioritizer) {
        this.reconciliationRepository = reconciliationRepository;
        this.recordRepository = recordRepository;
        this.breakRepository = breakRepository;
        this.exceptionPrioritizer = exceptionPrioritizer;
    }

    // AFTER_COMMIT is required, not just nice-to-have: the event is published from inside
    // ReconciliationService's transaction, before the matched-record status updates are committed.
    // A plain @EventListener would query in a new transaction that (under READ COMMITTED) still
    // sees the pre-match UNMATCHED status for every record, flagging matched records as breaks too.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReconciliationCompleted(ReconciliationCompletedEvent event) {
        Reconciliation reconciliation = reconciliationRepository.findById(event.reconciliationId()).orElse(null);
        if (reconciliation == null) {
            return;
        }
        ReconciliationTemplate template = reconciliation.getTemplate();
        if (template.getMatchRule() == null) {
            return;
        }

        YearMonth ym = YearMonth.parse(event.periodCode());
        List<RawTransactionRecord> unmatchedA = recordRepository.findBySourceSystemAndMatchStatusAndTransactionDateBetween(
                template.getMatchRule().getSourceSystemA(), RecordMatchStatus.UNMATCHED, ym.atDay(1), ym.atEndOfMonth());
        List<RawTransactionRecord> unmatchedB = recordRepository.findBySourceSystemAndMatchStatusAndTransactionDateBetween(
                template.getMatchRule().getSourceSystemB(), RecordMatchStatus.UNMATCHED, ym.atDay(1), ym.atEndOfMonth());

        unmatchedA.forEach(record -> createBreak(reconciliation, record));
        unmatchedB.forEach(record -> createBreak(reconciliation, record));
    }

    private void createBreak(Reconciliation reconciliation, RawTransactionRecord record) {
        Severity severity = exceptionPrioritizer.prioritize(record);

        ReconciliationBreak recBreak = new ReconciliationBreak();
        recBreak.setReconciliation(reconciliation);
        recBreak.setRecord(record);
        recBreak.setCategory(BreakCategory.MISSING_COUNTERPARTY);
        recBreak.setSeverity(severity);
        recBreak.setStatus(BreakStatus.OPEN);
        recBreak.setDescription("No matching counterparty record found for " + record.getSourceSystem()
                + " transaction " + record.getExternalId());
        recBreak.setSlaDueDate(LocalDate.now().plusDays(slaDaysFor(severity)));
        breakRepository.save(recBreak);
    }

    private int slaDaysFor(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 1;
            case HIGH -> 2;
            case MEDIUM -> 5;
            case LOW -> 10;
        };
    }
}
