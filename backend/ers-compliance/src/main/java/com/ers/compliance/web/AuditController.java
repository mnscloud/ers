package com.ers.compliance.web;

import com.ers.common.web.ApiResponse;
import com.ers.common.web.PageResponse;
import com.ers.compliance.domain.AuditEvent;
import com.ers.compliance.repository.AuditEventRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/compliance/audit-events")
@PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE')")
public class AuditController {

    private final AuditEventRepository auditEventRepository;

    public AuditController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditEvent>> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actor,
            Pageable pageable) {
        var page = Optional.ofNullable(entityType)
                .map(t -> auditEventRepository.findByEntityTypeIgnoreCaseOrderByOccurredAtDesc(t, pageable))
                .or(() -> Optional.ofNullable(actor)
                        .map(a -> auditEventRepository.findByActorIgnoreCaseOrderByOccurredAtDesc(a, pageable)))
                .orElseGet(() -> auditEventRepository.findAllByOrderByOccurredAtDesc(pageable));
        return ApiResponse.ok(PageResponse.from(page));
    }
}
