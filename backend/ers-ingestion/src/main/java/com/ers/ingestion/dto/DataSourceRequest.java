package com.ers.ingestion.dto;

import com.ers.ingestion.domain.DataSourceType;
import com.ers.ingestion.domain.FileFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DataSourceRequest(
        @NotBlank String name,
        @NotBlank String sourceSystem,
        @NotNull DataSourceType type,
        @NotNull FileFormat defaultFormat,
        String connectionConfig
) {
}
