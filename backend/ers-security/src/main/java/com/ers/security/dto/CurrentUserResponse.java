package com.ers.security.dto;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        List<String> roles,
        List<String> permissions
) {
}
