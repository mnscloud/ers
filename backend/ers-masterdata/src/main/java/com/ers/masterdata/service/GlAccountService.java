package com.ers.masterdata.service;

import com.ers.common.enums.ApprovalStatus;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.compliance.service.ApprovalService;
import com.ers.masterdata.domain.GlAccount;
import com.ers.masterdata.dto.MasterDataRequest;
import com.ers.masterdata.repository.GlAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GlAccountService {

    private final GlAccountRepository repository;
    private final ApprovalService approvalService;

    public GlAccountService(GlAccountRepository repository, ApprovalService approvalService) {
        this.repository = repository;
        this.approvalService = approvalService;
    }

    @Transactional
    public GlAccount create(MasterDataRequest request, String requestedBy) {
        GlAccount entity = new GlAccount();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setActive(false);
        entity.setApprovalStatus(ApprovalStatus.PENDING);
        entity = repository.save(entity);

        var approval = approvalService.requestApproval("GlAccount", entity.getId().toString(),
                "CREATE", requestedBy, null);
        entity.setApprovalRequestId(approval.getId());
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<GlAccount> list() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public GlAccount get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("GlAccount", id));
    }

    @Transactional
    public GlAccount approve(UUID id, String decidedBy, String comment) {
        GlAccount entity = get(id);
        approvalService.approve(entity.getApprovalRequestId(), decidedBy, comment);
        entity.setApprovalStatus(ApprovalStatus.APPROVED);
        entity.setActive(true);
        return repository.save(entity);
    }

    @Transactional
    public GlAccount reject(UUID id, String decidedBy, String comment) {
        GlAccount entity = get(id);
        approvalService.reject(entity.getApprovalRequestId(), decidedBy, comment);
        entity.setApprovalStatus(ApprovalStatus.REJECTED);
        return repository.save(entity);
    }
}
