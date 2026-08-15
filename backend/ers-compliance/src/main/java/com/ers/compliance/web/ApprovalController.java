package com.ers.compliance.web;

import com.ers.common.enums.ApprovalStatus;
import com.ers.common.web.ApiResponse;
import com.ers.common.web.PageResponse;
import com.ers.compliance.domain.ApprovalRequest;
import com.ers.compliance.dto.DecisionRequest;
import com.ers.compliance.service.ApprovalService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/compliance/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADJUSTMENT_APPROVE')")
    public ApiResponse<PageResponse<ApprovalRequest>> list(
            @RequestParam(defaultValue = "PENDING") ApprovalStatus status, Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(approvalService.findByStatus(status, pageable)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ADJUSTMENT_APPROVE')")
    public ApiResponse<ApprovalRequest> approve(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                                 Authentication authentication) {
        return ApiResponse.ok(approvalService.approve(id, authentication.getName(), request.comment()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ADJUSTMENT_APPROVE')")
    public ApiResponse<ApprovalRequest> reject(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                                Authentication authentication) {
        return ApiResponse.ok(approvalService.reject(id, authentication.getName(), request.comment()));
    }
}
