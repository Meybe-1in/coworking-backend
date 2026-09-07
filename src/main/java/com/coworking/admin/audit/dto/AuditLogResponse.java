package com.coworking.admin.audit.dto;

import com.coworking.admin.audit.enums.AuditAction;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long adminId,
        String adminName,
        AuditAction action,
        String entityType,
        Long entityId,
        Instant createdAt
) {
}