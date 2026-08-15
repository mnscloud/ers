package com.ers.masterdata.web;

import com.ers.common.web.ApiResponse;
import com.ers.masterdata.domain.TransactionType;
import com.ers.masterdata.dto.DecisionRequest;
import com.ers.masterdata.dto.MasterDataRequest;
import com.ers.masterdata.service.TransactionTypeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/masterdata/transaction-types")
public class TransactionTypeController {

    private final TransactionTypeService service;

    public TransactionTypeController(TransactionTypeService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<TransactionType>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MASTERDATA_WRITE')")
    public ApiResponse<TransactionType> create(@Valid @RequestBody MasterDataRequest request, Authentication authentication) {
        return ApiResponse.ok(service.create(request, authentication.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<TransactionType> approve(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                                 Authentication authentication) {
        return ApiResponse.ok(service.approve(id, authentication.getName(), request.comment()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<TransactionType> reject(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                                Authentication authentication) {
        return ApiResponse.ok(service.reject(id, authentication.getName(), request.comment()));
    }
}
