package com.ers.adjustment.web;

import com.ers.adjustment.domain.JournalEntry;
import com.ers.adjustment.dto.CreateJournalEntryRequest;
import com.ers.adjustment.dto.DecisionRequest;
import com.ers.adjustment.service.AdjustmentService;
import com.ers.common.web.ApiResponse;
import com.ers.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/adjustments")
public class AdjustmentController {

    private final AdjustmentService adjustmentService;

    public AdjustmentController(AdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
    }

    @GetMapping
    public ApiResponse<PageResponse<JournalEntry>> list(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(adjustmentService.list(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<JournalEntry> get(@PathVariable UUID id) {
        return ApiResponse.ok(adjustmentService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADJUSTMENT_CREATE')")
    public ApiResponse<JournalEntry> create(@Valid @RequestBody CreateJournalEntryRequest request,
                                             Authentication authentication) {
        return ApiResponse.ok(adjustmentService.create(request, authentication.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ADJUSTMENT_APPROVE')")
    public ApiResponse<JournalEntry> approve(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                              Authentication authentication) {
        return ApiResponse.ok(adjustmentService.approveAndPost(id, authentication.getName(), request.comment()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ADJUSTMENT_APPROVE')")
    public ApiResponse<JournalEntry> reject(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                             Authentication authentication) {
        return ApiResponse.ok(adjustmentService.reject(id, authentication.getName(), request.comment()));
    }
}
