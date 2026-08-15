package com.ers.reconciliation.service;

import com.ers.common.enums.ApprovalStatus;
import com.ers.common.enums.ReconciliationType;
import com.ers.common.event.ReconciliationCompletedEvent;
import com.ers.common.exception.BusinessException;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.compliance.service.PeriodLockService;
import com.ers.matching.domain.MatchRun;
import com.ers.matching.service.MatchingEngine;
import com.ers.reconciliation.domain.ReconStatus;
import com.ers.reconciliation.domain.Reconciliation;
import com.ers.reconciliation.domain.ReconciliationTemplate;
import com.ers.reconciliation.dto.CreateReconciliationRequest;
import com.ers.reconciliation.repository.ReconciliationRepository;
import com.ers.reconciliation.repository.ReconciliationTemplateRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;
    private final ReconciliationTemplateRepository templateRepository;
    private final MatchingEngine matchingEngine;
    private final PeriodLockService periodLockService;
    private final ApplicationEventPublisher eventPublisher;

    public ReconciliationService(ReconciliationRepository reconciliationRepository,
                                  ReconciliationTemplateRepository templateRepository,
                                  MatchingEngine matchingEngine,
                                  PeriodLockService periodLockService,
                                  ApplicationEventPublisher eventPublisher) {
        this.reconciliationRepository = reconciliationRepository;
        this.templateRepository = templateRepository;
        this.matchingEngine = matchingEngine;
        this.periodLockService = periodLockService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Reconciliation create(CreateReconciliationRequest request) {
        ReconciliationTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> ResourceNotFoundException.of("ReconciliationTemplate", request.templateId()));
        if (!template.isActive() || template.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("TEMPLATE_NOT_APPROVED", "Template " + template.getName() + " has not been approved yet");
        }

        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setTemplate(template);
        reconciliation.setType(template.getType());
        reconciliation.setPeriodCode(request.periodCode());
        reconciliation.setStatus(ReconStatus.OPEN);
        return reconciliationRepository.save(reconciliation);
    }

    @Transactional(readOnly = true)
    public Page<Reconciliation> list(Pageable pageable) {
        return reconciliationRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Reconciliation get(UUID id) {
        return reconciliationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Reconciliation", id));
    }

    @Transactional
    public Reconciliation trigger(UUID id) {
        Reconciliation reconciliation = get(id);
        periodLockService.assertOpen(reconciliation.getPeriodCode());

        if (reconciliation.getType() != ReconciliationType.BANK_CASH) {
            throw new BusinessException("NOT_IMPLEMENTED",
                    reconciliation.getType() + " comparison logic is not implemented yet - only Bank & Cash is wired to the matching engine in this release");
        }
        ReconciliationTemplate template = reconciliation.getTemplate();
        if (template.getMatchRule() == null) {
            throw new BusinessException("NO_MATCH_RULE", "Template " + template.getName() + " has no match rule configured");
        }

        reconciliation.setStatus(ReconStatus.IN_PROGRESS);
        reconciliationRepository.save(reconciliation);

        MatchRun run = matchingEngine.run(template.getMatchRule().getId(), reconciliation.getPeriodCode());

        reconciliation.setMatchRun(run);
        reconciliation.setMatchedCount(run.getMatchedCount());
        reconciliation.setUnmatchedCount(run.getUnmatchedCountA() + run.getUnmatchedCountB());
        reconciliation.setStatus(ReconStatus.COMPLETED);
        reconciliation.setCompletedAt(Instant.now());
        Reconciliation saved = reconciliationRepository.save(reconciliation);

        eventPublisher.publishEvent(new ReconciliationCompletedEvent(saved.getId(), saved.getPeriodCode()));

        return saved;
    }
}
