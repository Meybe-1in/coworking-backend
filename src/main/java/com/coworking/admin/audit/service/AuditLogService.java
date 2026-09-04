package com.coworking.admin.audit.service;

import com.coworking.admin.audit.dto.AuditLogRequest;
import com.coworking.admin.audit.dto.AuditLogResponse;
import com.coworking.admin.audit.enums.AuditAction;
import com.coworking.admin.dto.AdminPageResponse;

public interface AuditLogService {

    void log(
            AuditAction action,
            String entityType,
            Long entityId
    );

    AdminPageResponse<AuditLogResponse> getAuditLogs(
            AuditLogRequest request,
            int page,
            int size
    );
}
