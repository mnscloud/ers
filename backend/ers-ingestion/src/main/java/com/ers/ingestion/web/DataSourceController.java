package com.ers.ingestion.web;

import com.ers.common.web.ApiResponse;
import com.ers.ingestion.domain.DataSource;
import com.ers.ingestion.dto.DataSourceRequest;
import com.ers.ingestion.dto.DecisionRequest;
import com.ers.ingestion.service.DataSourceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ingestion/data-sources")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @GetMapping
    public ApiResponse<List<DataSource>> list() {
        return ApiResponse.ok(dataSourceService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INGESTION_WRITE')")
    public ApiResponse<DataSource> create(@Valid @RequestBody DataSourceRequest request, Authentication authentication) {
        return ApiResponse.ok(dataSourceService.create(request, authentication.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<DataSource> approve(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                            Authentication authentication) {
        return ApiResponse.ok(dataSourceService.approve(id, authentication.getName(), request.comment()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MASTERDATA_APPROVE')")
    public ApiResponse<DataSource> reject(@PathVariable UUID id, @RequestBody DecisionRequest request,
                                           Authentication authentication) {
        return ApiResponse.ok(dataSourceService.reject(id, authentication.getName(), request.comment()));
    }
}
