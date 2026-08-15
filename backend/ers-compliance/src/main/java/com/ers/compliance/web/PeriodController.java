package com.ers.compliance.web;

import com.ers.common.web.ApiResponse;
import com.ers.compliance.domain.AccountingPeriod;
import com.ers.compliance.service.PeriodLockService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compliance/periods")
public class PeriodController {

    private final PeriodLockService periodLockService;

    public PeriodController(PeriodLockService periodLockService) {
        this.periodLockService = periodLockService;
    }

    @GetMapping
    public ApiResponse<List<AccountingPeriod>> list() {
        return ApiResponse.ok(periodLockService.list());
    }

    @PostMapping("/{periodCode}/lock")
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE')")
    public ApiResponse<AccountingPeriod> lock(@PathVariable String periodCode) {
        return ApiResponse.ok(periodLockService.lock(periodCode));
    }

    @PostMapping("/{periodCode}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE')")
    public ApiResponse<AccountingPeriod> unlock(@PathVariable String periodCode) {
        return ApiResponse.ok(periodLockService.unlock(periodCode));
    }
}
