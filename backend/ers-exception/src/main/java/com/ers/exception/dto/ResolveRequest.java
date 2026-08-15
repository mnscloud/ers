package com.ers.exception.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveRequest(@NotBlank String resolutionComment) {
}
