package com.ers.masterdata.web;

import com.ers.common.web.ApiResponse;
import com.ers.masterdata.domain.GlAccount;
import com.ers.masterdata.dto.DecisionRequest;
import com.ers.masterdata.dto.MasterDataRequest;
import com.ers.masterdata.service.GlAccountService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/masterdata/gl-accounts")
public class GlAccountController {

    private final GlAccountService service;

    public GlAccountController(GlAccountService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<GlAccount>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MASTERDATA_WRITE')")
    public ApiResponse<GlAccount> create(@Valid @RequestBody MasterDataRequest request, Authentication authentication) {
        return ApiResponse.ok(service.create(request, authentication.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<GlAccount> approve(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                           Authentication authentication) {
        return ApiResponse.ok(service.approve(id, authentication.getName(), request.comment()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<GlAccount> reject(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                          Authentication authentication) {
        return ApiResponse.ok(service.reject(id, authentication.getName(), request.comment()));
    }
}
