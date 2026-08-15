package com.ers.masterdata.web;

import com.ers.common.web.ApiResponse;
import com.ers.masterdata.domain.Currency;
import com.ers.masterdata.dto.DecisionRequest;
import com.ers.masterdata.dto.MasterDataRequest;
import com.ers.masterdata.service.CurrencyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/masterdata/currencies")
public class CurrencyController {

    private final CurrencyService service;

    public CurrencyController(CurrencyService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Currency>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MASTERDATA_WRITE')")
    public ApiResponse<Currency> create(@Valid @RequestBody MasterDataRequest request, Authentication authentication) {
        return ApiResponse.ok(service.create(request, authentication.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<Currency> approve(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                          Authentication authentication) {
        return ApiResponse.ok(service.approve(id, authentication.getName(), request.comment()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<Currency> reject(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                         Authentication authentication) {
        return ApiResponse.ok(service.reject(id, authentication.getName(), request.comment()));
    }
}
