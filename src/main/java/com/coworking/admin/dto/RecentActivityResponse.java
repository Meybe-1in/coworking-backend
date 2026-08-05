package com.coworking.admin.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record RecentActivityResponse(
        String type,
        String description,
        Instant date,
        String user
) {
}
