package com.ers.ingestion.web;

import com.ers.common.web.ApiResponse;
import com.ers.common.web.PageResponse;
import com.ers.ingestion.domain.FileFormat;
import com.ers.ingestion.domain.IngestionBatch;
import com.ers.ingestion.domain.RawTransactionRecord;
import com.ers.ingestion.repository.IngestionBatchRepository;
import com.ers.ingestion.repository.RawTransactionRecordRepository;
import com.ers.ingestion.service.IngestionService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {

    private final IngestionService ingestionService;
    private final RawTransactionRecordRepository recordRepository;
    private final IngestionBatchRepository batchRepository;

    public IngestionController(IngestionService ingestionService, RawTransactionRecordRepository recordRepository,
                                IngestionBatchRepository batchRepository) {
        this.ingestionService = ingestionService;
        this.recordRepository = recordRepository;
        this.batchRepository = batchRepository;
    }

    @GetMapping("/batches")
    public ApiResponse<PageResponse<IngestionBatch>> listBatches(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(batchRepository.findAllByOrderByCreatedAtDesc(pageable)));
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('INGESTION_WRITE')")
    public ApiResponse<IngestionBatch> upload(@RequestParam UUID dataSourceId,
                                               @RequestParam FileFormat format,
                                               @RequestParam MultipartFile file) {
        return ApiResponse.ok(ingestionService.upload(dataSourceId, format, file));
    }

    @GetMapping("/batches/{id}")
    public ApiResponse<IngestionBatch> getBatch(@PathVariable UUID id) {
        return ApiResponse.ok(ingestionService.getBatch(id));
    }

    @GetMapping("/batches/{id}/records")
    public ApiResponse<PageResponse<RawTransactionRecord>> getBatchRecords(@PathVariable UUID id, Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(recordRepository.findByBatchId(id, pageable)));
    }
}
