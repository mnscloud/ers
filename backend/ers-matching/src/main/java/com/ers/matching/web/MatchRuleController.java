package com.ers.matching.web;

import com.ers.common.web.ApiResponse;
import com.ers.matching.domain.MatchRule;
import com.ers.matching.dto.DecisionRequest;
import com.ers.matching.dto.MatchRuleRequest;
import com.ers.matching.service.MatchRuleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matching/rules")
public class MatchRuleController {

    private final MatchRuleService matchRuleService;

    public MatchRuleController(MatchRuleService matchRuleService) {
        this.matchRuleService = matchRuleService;
    }

    @GetMapping
    public ApiResponse<List<MatchRule>> list() {
        return ApiResponse.ok(matchRuleService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<MatchRule> get(@PathVariable UUID id) {
        return ApiResponse.ok(matchRuleService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATCHING_CONFIGURE')")
    public ApiResponse<MatchRule> create(@Valid @RequestBody MatchRuleRequest request, Authentication authentication) {
        return ApiResponse.ok(matchRuleService.create(request, authentication.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<MatchRule> approve(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                           Authentication authentication) {
        return ApiResponse.ok(matchRuleService.approve(id, authentication.getName(), request.comment()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<MatchRule> reject(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                          Authentication authentication) {
        return ApiResponse.ok(matchRuleService.reject(id, authentication.getName(), request.comment()));
    }
}
