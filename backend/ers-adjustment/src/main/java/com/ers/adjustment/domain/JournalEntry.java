package com.ers.adjustment.domain;

import com.ers.common.domain.BaseEntity;
import com.ers.exception.domain.ReconciliationBreak;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
public class JournalEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_break_id")
    private ReconciliationBreak reconciliationBreak;

    @Column(nullable = false, length = 50)
    private String accountCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DebitCredit debitCredit;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 500)
    private String description;

    /** e.g. "2026-07" */
    @Column(nullable = false, length = 7)
    private String periodCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JournalEntryStatus status = JournalEntryStatus.DRAFT;

    private UUID approvalRequestId;

    private String erpReference;

    private Instant postedAt;
}
