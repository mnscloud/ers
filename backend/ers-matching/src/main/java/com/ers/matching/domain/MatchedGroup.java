package com.ers.matching.domain;

import com.ers.common.domain.BaseEntity;
import com.ers.common.enums.MatchType;
import com.ers.ingestion.domain.RawTransactionRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "matched_groups")
@Getter
@Setter
@NoArgsConstructor
public class MatchedGroup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_run_id", nullable = false)
    private MatchRun matchRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchType matchType;

    @Column(nullable = false)
    private double confidenceScore;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "matched_group_records",
            joinColumns = @JoinColumn(name = "matched_group_id"),
            inverseJoinColumns = @JoinColumn(name = "record_id")
    )
    private Set<RawTransactionRecord> records = new HashSet<>();
}
