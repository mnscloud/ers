package com.ers.ingestion.domain;

import com.ers.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "raw_transaction_records", indexes = {
        @Index(name = "idx_rtr_source_system", columnList = "sourceSystem"),
        @Index(name = "idx_rtr_match_status", columnList = "matchStatus"),
        @Index(name = "idx_rtr_transaction_date", columnList = "transactionDate")
})
@Getter
@Setter
@NoArgsConstructor
public class RawTransactionRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private IngestionBatch batch;

    @Column(nullable = false, length = 100)
    private String sourceSystem;

    @Column(nullable = false, length = 150)
    private String externalId;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordMatchStatus matchStatus = RecordMatchStatus.UNMATCHED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String rawPayload;
}
