package com.ers.matching.web;

import com.ers.common.web.ApiResponse;
import com.ers.common.web.PageResponse;
import com.ers.matching.domain.MatchRun;
import com.ers.matching.domain.MatchedGroup;
import com.ers.matching.dto.RunMatchRequest;
import com.ers.matching.repository.MatchRunRepository;
import com.ers.matching.repository.MatchedGroupRepository;
import com.ers.matching.service.MatchingEngine;
import com.ers.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/matching/runs")
public class MatchRunController {

    private final MatchingEngine matchingEngine;
    private final MatchRunRepository matchRunRepository;
    private final MatchedGroupRepository matchedGroupRepository;

    public MatchRunController(MatchingEngine matchingEngine, MatchRunRepository matchRunRepository,
                               MatchedGroupRepository matchedGroupRepository) {
        this.matchingEngine = matchingEngine;
        this.matchRunRepository = matchRunRepository;
        this.matchedGroupRepository = matchedGroupRepository;
    }

    @GetMapping
    public ApiResponse<PageResponse<MatchRun>> list(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(matchRunRepository.findAllByOrderByCreatedAtDesc(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<MatchRun> get(@PathVariable UUID id) {
        return ApiResponse.ok(matchRunRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("MatchRun", id)));
    }

    @GetMapping("/{id}/results")
    public ApiResponse<PageResponse<MatchedGroup>> results(@PathVariable UUID id, Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(matchedGroupRepository.findByMatchRunId(id, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATCHING_RUN')")
    public ApiResponse<MatchRun> run(@Valid @RequestBody RunMatchRequest request) {
        return ApiResponse.ok(matchingEngine.run(request.matchRuleId(), request.periodCode()));
    }
}
