package com.coworking.admin.dto;

import java.util.Set;

public record AdminProfileResponse(
        Long id,
        String username,
        String email,
        Set<String> roles
) {
}
