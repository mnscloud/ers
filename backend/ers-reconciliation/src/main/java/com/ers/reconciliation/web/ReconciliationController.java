package com.ers.reconciliation.web;

import com.ers.common.web.ApiResponse;
import com.ers.common.web.PageResponse;
import com.ers.reconciliation.domain.Reconciliation;
import com.ers.reconciliation.dto.CreateReconciliationRequest;
import com.ers.reconciliation.service.ReconciliationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reconciliations")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<Reconciliation>> list(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(reconciliationService.list(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Reconciliation> get(@PathVariable UUID id) {
        return ApiResponse.ok(reconciliationService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATCHING_RUN')")
    public ApiResponse<Reconciliation> create(@Valid @RequestBody CreateReconciliationRequest request) {
        return ApiResponse.ok(reconciliationService.create(request));
    }

    @PostMapping("/{id}/trigger")
    @PreAuthorize("hasAuthority('MATCHING_RUN')")
    public ApiResponse<Reconciliation> trigger(@PathVariable UUID id) {
        return ApiResponse.ok(reconciliationService.trigger(id));
    }
}
