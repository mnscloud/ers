package com.ers.exception.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRequest(@NotBlank String assignee) {
}
