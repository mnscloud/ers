package com.ers.security.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        boolean enabled,
        boolean accountLocked,
        Instant lastLoginAt,
        List<String> roles
) {
}
