package com.ers.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Shared create-request shape for all four master-data entity types
 * (Transaction Type, GL Account, Currency, Counterparty) - they all
 * take the same code/name/description fields.
 */
public record MasterDataRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description
) {
}
