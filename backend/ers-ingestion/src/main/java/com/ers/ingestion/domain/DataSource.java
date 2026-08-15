package com.ers.ingestion.domain;

import com.ers.common.domain.ApprovableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "data_sources")
@Getter
@Setter
@NoArgsConstructor
public class DataSource extends ApprovableEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Logical origin system this source represents, e.g. CHASE_BANK, SAP_GL, SALESFORCE_CRM. */
    @Column(nullable = false, length = 100)
    private String sourceSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataSourceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FileFormat defaultFormat;

    @Column(length = 1000)
    private String connectionConfig;

    @Column(nullable = false)
    private boolean active = false;
}
