package com.ers.reconciliation.web;

import com.ers.common.web.ApiResponse;
import com.ers.reconciliation.domain.ReconciliationTemplate;
import com.ers.reconciliation.dto.DecisionRequest;
import com.ers.reconciliation.dto.ReconciliationTemplateRequest;
import com.ers.reconciliation.service.ReconciliationTemplateService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reconciliation/templates")
public class ReconciliationTemplateController {

    private final ReconciliationTemplateService templateService;

    public ReconciliationTemplateController(ReconciliationTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ApiResponse<List<ReconciliationTemplate>> list() {
        return ApiResponse.ok(templateService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ReconciliationTemplate> get(@PathVariable UUID id) {
        return ApiResponse.ok(templateService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATCHING_CONFIGURE')")
    public ApiResponse<ReconciliationTemplate> create(@Valid @RequestBody ReconciliationTemplateRequest request,
                                                        Authentication authentication) {
        return ApiResponse.ok(templateService.create(request, authentication.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<ReconciliationTemplate> approve(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                                        Authentication authentication) {
        return ApiResponse.ok(templateService.approve(id, authentication.getName(), request.comment()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<ReconciliationTemplate> reject(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                                       Authentication authentication) {
        return ApiResponse.ok(templateService.reject(id, authentication.getName(), request.comment()));
    }
}
