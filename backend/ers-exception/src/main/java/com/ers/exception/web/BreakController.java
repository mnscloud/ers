package com.ers.exception.web;

import com.ers.common.web.ApiResponse;
import com.ers.common.web.PageResponse;
import com.ers.exception.domain.BreakStatus;
import com.ers.exception.domain.ReconciliationBreak;
import com.ers.exception.dto.AssignRequest;
import com.ers.exception.dto.ResolveRequest;
import com.ers.exception.service.BreakTriageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/exceptions")
public class BreakController {

    private final BreakTriageService triageService;

    public BreakController(BreakTriageService triageService) {
        this.triageService = triageService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReconciliationBreak>> list(
            @RequestParam(defaultValue = "OPEN") BreakStatus status, Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(triageService.findByStatus(status, pageable)));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('EXCEPTION_TRIAGE')")
    public ApiResponse<ReconciliationBreak> assign(@PathVariable UUID id, @Valid @RequestBody AssignRequest request,
                                                     Authentication authentication) {
        return ApiResponse.ok(triageService.assign(id, request.assignee(), authentication.getName()));
    }

    @PatchMapping("/{id}/escalate")
    @PreAuthorize("hasAuthority('EXCEPTION_TRIAGE')")
    public ApiResponse<ReconciliationBreak> escalate(@PathVariable UUID id, Authentication authentication) {
        return ApiResponse.ok(triageService.escalate(id, authentication.getName()));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('EXCEPTION_TRIAGE')")
    public ApiResponse<ReconciliationBreak> resolve(@PathVariable UUID id, @Valid @RequestBody ResolveRequest request,
                                                      Authentication authentication) {
        return ApiResponse.ok(triageService.resolve(id, request.resolutionComment(), authentication.getName()));
    }
}
