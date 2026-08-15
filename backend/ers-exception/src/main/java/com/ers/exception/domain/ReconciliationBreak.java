package com.ers.exception.domain;

import com.ers.common.domain.BaseEntity;
import com.ers.common.enums.Severity;
import com.ers.ingestion.domain.RawTransactionRecord;
import com.ers.reconciliation.domain.Reconciliation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "reconciliation_breaks")
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationBreak extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private Reconciliation reconciliation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private RawTransactionRecord record;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BreakCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BreakStatus status = BreakStatus.OPEN;

    private String assignee;

    private LocalDate slaDueDate;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String resolutionComment;
}
