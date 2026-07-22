package com.coworking.admin.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AdminPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        long totalPages,
        boolean first,
        boolean last
) {
}
