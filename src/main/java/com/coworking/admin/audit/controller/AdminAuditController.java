package com.coworking.admin.audit.controller;

import com.coworking.admin.audit.dto.AuditLogRequest;
import com.coworking.admin.audit.dto.AuditLogResponse;
import com.coworking.admin.audit.service.AuditLogService;
import com.coworking.admin.dto.AdminPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<AdminPageResponse<AuditLogResponse>> getAuditLogs(
            @Valid AuditLogRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                auditLogService.getAuditLogs(request, page, size)
        );
    }

    //Csv export
    @PostMapping(
            value = "/export/csv",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "text/csv"
    )
    public ResponseEntity<byte[]> exportAuditLogsCsv(
            @Valid @RequestBody AuditLogRequest request
    ) {

        byte[] csv = auditLogService.exportAuditLogsCsv(request);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"audit-logs.csv\""
                )
                .contentType(
                        MediaType.parseMediaType("text/csv")
                )
                .body(csv);
    }
}