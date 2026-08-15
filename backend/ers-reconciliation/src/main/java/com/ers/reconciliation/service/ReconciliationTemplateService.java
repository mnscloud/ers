package com.ers.reconciliation.service;

import com.ers.common.enums.ApprovalStatus;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.compliance.service.ApprovalService;
import com.ers.matching.repository.MatchRuleRepository;
import com.ers.reconciliation.domain.ReconciliationTemplate;
import com.ers.reconciliation.dto.ReconciliationTemplateRequest;
import com.ers.reconciliation.repository.ReconciliationTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReconciliationTemplateService {

    private final ReconciliationTemplateRepository templateRepository;
    private final MatchRuleRepository matchRuleRepository;
    private final ApprovalService approvalService;

    public ReconciliationTemplateService(ReconciliationTemplateRepository templateRepository,
                                          MatchRuleRepository matchRuleRepository,
                                          ApprovalService approvalService) {
        this.templateRepository = templateRepository;
        this.matchRuleRepository = matchRuleRepository;
        this.approvalService = approvalService;
    }

    @Transactional
    public ReconciliationTemplate create(ReconciliationTemplateRequest request, String requestedBy) {
        ReconciliationTemplate template = new ReconciliationTemplate();
        template.setName(request.name());
        template.setType(request.type());
        template.setOwner(request.owner());
        if (request.matchRuleId() != null) {
            template.setMatchRule(matchRuleRepository.findById(request.matchRuleId())
                    .orElseThrow(() -> ResourceNotFoundException.of("MatchRule", request.matchRuleId())));
        }
        template.setActive(false);
        template.setApprovalStatus(ApprovalStatus.PENDING);
        template = templateRepository.save(template);

        var approval = approvalService.requestApproval("ReconciliationTemplate", template.getId().toString(),
                "CREATE", requestedBy, null);
        template.setApprovalRequestId(approval.getId());
        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationTemplate> list() {
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ReconciliationTemplate get(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ReconciliationTemplate", id));
    }

    @Transactional
    public ReconciliationTemplate approve(UUID id, String decidedBy, String comment) {
        ReconciliationTemplate template = get(id);
        approvalService.approve(template.getApprovalRequestId(), decidedBy, comment);
        template.setApprovalStatus(ApprovalStatus.APPROVED);
        template.setActive(true);
        return templateRepository.save(template);
    }

    @Transactional
    public ReconciliationTemplate reject(UUID id, String decidedBy, String comment) {
        ReconciliationTemplate template = get(id);
        approvalService.reject(template.getApprovalRequestId(), decidedBy, comment);
        template.setApprovalStatus(ApprovalStatus.REJECTED);
        return templateRepository.save(template);
    }
}
