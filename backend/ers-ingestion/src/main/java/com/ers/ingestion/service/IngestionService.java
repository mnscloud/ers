package com.ers.ingestion.service;

import com.ers.common.enums.ApprovalStatus;
import com.ers.common.enums.AuditAction;
import com.ers.common.event.AuditLogEvent;
import com.ers.common.exception.BusinessException;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.ingestion.domain.BatchStatus;
import com.ers.ingestion.domain.DataSource;
import com.ers.ingestion.domain.FileFormat;
import com.ers.ingestion.domain.IngestionBatch;
import com.ers.ingestion.domain.RawTransactionRecord;
import com.ers.ingestion.parser.NormalizedRecord;
import com.ers.ingestion.parser.RecordParser;
import com.ers.ingestion.repository.DataSourceRepository;
import com.ers.ingestion.repository.IngestionBatchRepository;
import com.ers.ingestion.repository.RawTransactionRecordRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IngestionService {

    private final DataSourceRepository dataSourceRepository;
    private final IngestionBatchRepository batchRepository;
    private final RawTransactionRecordRepository recordRepository;
    private final Map<FileFormat, RecordParser> parsersByFormat;
    private final ApplicationEventPublisher eventPublisher;

    public IngestionService(DataSourceRepository dataSourceRepository,
                             IngestionBatchRepository batchRepository,
                             RawTransactionRecordRepository recordRepository,
                             List<RecordParser> parsers,
                             ApplicationEventPublisher eventPublisher) {
        this.dataSourceRepository = dataSourceRepository;
        this.batchRepository = batchRepository;
        this.recordRepository = recordRepository;
        this.parsersByFormat = parsers.stream()
                .collect(Collectors.toMap(RecordParser::supportedFormat, Function.identity()));
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public IngestionBatch upload(UUID dataSourceId, FileFormat format, MultipartFile file) {
        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> ResourceNotFoundException.of("DataSource", dataSourceId));
        if (!dataSource.isActive() || dataSource.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("DATA_SOURCE_NOT_APPROVED",
                    "Data source " + dataSource.getName() + " has not been approved yet");
        }

        RecordParser parser = parsersByFormat.get(format);
        if (parser == null) {
            throw new BusinessException("UNSUPPORTED_FORMAT", "No parser registered for format " + format);
        }

        IngestionBatch batch = new IngestionBatch();
        batch.setDataSource(dataSource);
        batch.setFileName(file.getOriginalFilename());
        batch.setFormat(format);
        batch.setStatus(BatchStatus.PROCESSING);
        batch.setStartedAt(Instant.now());
        batch = batchRepository.save(batch);

        List<NormalizedRecord> parsed;
        try (var inputStream = file.getInputStream()) {
            parsed = parser.parse(inputStream);
        } catch (IOException ex) {
            batch.setStatus(BatchStatus.FAILED);
            batch.setErrorMessage(ex.getMessage());
            batch.setCompletedAt(Instant.now());
            return batchRepository.save(batch);
        } catch (BusinessException ex) {
            batch.setStatus(BatchStatus.FAILED);
            batch.setErrorMessage(ex.getMessage());
            batch.setCompletedAt(Instant.now());
            return batchRepository.save(batch);
        }

        int success = 0;
        int failed = 0;
        for (NormalizedRecord normalized : parsed) {
            try {
                RawTransactionRecord record = new RawTransactionRecord();
                record.setBatch(batch);
                record.setSourceSystem(dataSource.getSourceSystem());
                record.setExternalId(normalized.externalId());
                record.setTransactionDate(normalized.transactionDate());
                record.setAmount(normalized.amount());
                record.setCurrency(normalized.currency());
                record.setDescription(normalized.description());
                record.setRawPayload(normalized.rawPayloadJson());
                recordRepository.save(record);
                success++;
            } catch (RuntimeException ex) {
                failed++;
            }
        }

        batch.setTotalRecords(parsed.size());
        batch.setSuccessRecords(success);
        batch.setFailedRecords(failed);
        batch.setStatus(failed == 0 ? BatchStatus.COMPLETED : (success == 0 ? BatchStatus.FAILED : BatchStatus.PARTIAL));
        batch.setCompletedAt(Instant.now());
        IngestionBatch saved = batchRepository.save(batch);

        eventPublisher.publishEvent(AuditLogEvent.of(currentUser(), AuditAction.UPLOAD, "IngestionBatch",
                saved.getId().toString(),
                "Uploaded " + saved.getFileName() + " (" + success + " ok / " + failed + " failed)"));

        return saved;
    }

    @Transactional(readOnly = true)
    public IngestionBatch getBatch(UUID batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> ResourceNotFoundException.of("IngestionBatch", batchId));
    }

    private String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
