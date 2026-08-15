package com.ers.adjustment.service;

import com.ers.adjustment.domain.JournalEntry;
import com.ers.adjustment.domain.JournalEntryStatus;
import com.ers.adjustment.dto.CreateJournalEntryRequest;
import com.ers.adjustment.erp.ErpPostingClient;
import com.ers.adjustment.repository.JournalEntryRepository;
import com.ers.common.exception.BusinessException;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.compliance.service.ApprovalService;
import com.ers.compliance.service.PeriodLockService;
import com.ers.exception.repository.ReconciliationBreakRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AdjustmentService {

    private final JournalEntryRepository journalEntryRepository;
    private final ReconciliationBreakRepository breakRepository;
    private final PeriodLockService periodLockService;
    private final ApprovalService approvalService;
    private final ErpPostingClient erpPostingClient;

    public AdjustmentService(JournalEntryRepository journalEntryRepository,
                              ReconciliationBreakRepository breakRepository,
                              PeriodLockService periodLockService,
                              ApprovalService approvalService,
                              ErpPostingClient erpPostingClient) {
        this.journalEntryRepository = journalEntryRepository;
        this.breakRepository = breakRepository;
        this.periodLockService = periodLockService;
        this.approvalService = approvalService;
        this.erpPostingClient = erpPostingClient;
    }

    @Transactional
    public JournalEntry create(CreateJournalEntryRequest request, String requestedBy) {
        periodLockService.assertOpen(request.periodCode());

        JournalEntry entry = new JournalEntry();
        if (request.reconciliationBreakId() != null) {
            entry.setReconciliationBreak(breakRepository.findById(request.reconciliationBreakId())
                    .orElseThrow(() -> ResourceNotFoundException.of("ReconciliationBreak", request.reconciliationBreakId())));
        }
        entry.setAccountCode(request.accountCode());
        entry.setDebitCredit(request.debitCredit());
        entry.setAmount(request.amount());
        entry.setCurrency(request.currency());
        entry.setDescription(request.description());
        entry.setPeriodCode(request.periodCode());
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry = journalEntryRepository.save(entry);

        var approval = approvalService.requestApproval("JournalEntry", entry.getId().toString(),
                "POST_JOURNAL", requestedBy, request.description());
        entry.setApprovalRequestId(approval.getId());
        entry.setStatus(JournalEntryStatus.PENDING_APPROVAL);
        return journalEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<JournalEntry> list(Pageable pageable) {
        return journalEntryRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public JournalEntry get(UUID id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("JournalEntry", id));
    }

    @Transactional
    public JournalEntry approveAndPost(UUID id, String decidedBy, String comment) {
        JournalEntry entry = get(id);
        requirePendingApproval(entry);

        periodLockService.assertOpen(entry.getPeriodCode());
        approvalService.approve(entry.getApprovalRequestId(), decidedBy, comment);
        entry.setStatus(JournalEntryStatus.APPROVED);

        ErpPostingClient.ErpPostingResult result = erpPostingClient.post(entry);
        if (result.success()) {
            entry.setStatus(JournalEntryStatus.POSTED);
            entry.setErpReference(result.erpReference());
            entry.setPostedAt(Instant.now());
        }
        return journalEntryRepository.save(entry);
    }

    @Transactional
    public JournalEntry reject(UUID id, String decidedBy, String comment) {
        JournalEntry entry = get(id);
        requirePendingApproval(entry);

        approvalService.reject(entry.getApprovalRequestId(), decidedBy, comment);
        entry.setStatus(JournalEntryStatus.REJECTED);
        return journalEntryRepository.save(entry);
    }

    private void requirePendingApproval(JournalEntry entry) {
        if (entry.getStatus() != JournalEntryStatus.PENDING_APPROVAL) {
            throw new BusinessException("INVALID_STATE", "Journal entry is not pending approval (current status: " + entry.getStatus() + ")");
        }
    }
}
