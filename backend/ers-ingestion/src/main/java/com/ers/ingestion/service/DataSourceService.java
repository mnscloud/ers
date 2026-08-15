package com.ers.ingestion.service;

import com.ers.common.enums.ApprovalStatus;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.compliance.service.ApprovalService;
import com.ers.ingestion.domain.DataSource;
import com.ers.ingestion.dto.DataSourceRequest;
import com.ers.ingestion.repository.DataSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final ApprovalService approvalService;

    public DataSourceService(DataSourceRepository dataSourceRepository, ApprovalService approvalService) {
        this.dataSourceRepository = dataSourceRepository;
        this.approvalService = approvalService;
    }

    @Transactional
    public DataSource create(DataSourceRequest request, String requestedBy) {
        DataSource dataSource = new DataSource();
        dataSource.setName(request.name());
        dataSource.setSourceSystem(request.sourceSystem());
        dataSource.setType(request.type());
        dataSource.setDefaultFormat(request.defaultFormat());
        dataSource.setConnectionConfig(request.connectionConfig());
        dataSource.setActive(false);
        dataSource.setApprovalStatus(ApprovalStatus.PENDING);
        dataSource = dataSourceRepository.save(dataSource);

        var approval = approvalService.requestApproval("DataSource", dataSource.getId().toString(),
                "CREATE", requestedBy, null);
        dataSource.setApprovalRequestId(approval.getId());
        return dataSourceRepository.save(dataSource);
    }

    @Transactional(readOnly = true)
    public List<DataSource> list() {
        return dataSourceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public DataSource get(UUID id) {
        return dataSourceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("DataSource", id));
    }

    @Transactional
    public DataSource approve(UUID id, String decidedBy, String comment) {
        DataSource dataSource = get(id);
        approvalService.approve(dataSource.getApprovalRequestId(), decidedBy, comment);
        dataSource.setApprovalStatus(ApprovalStatus.APPROVED);
        dataSource.setActive(true);
        return dataSourceRepository.save(dataSource);
    }

    @Transactional
    public DataSource reject(UUID id, String decidedBy, String comment) {
        DataSource dataSource = get(id);
        approvalService.reject(dataSource.getApprovalRequestId(), decidedBy, comment);
        dataSource.setApprovalStatus(ApprovalStatus.REJECTED);
        return dataSourceRepository.save(dataSource);
    }
}
