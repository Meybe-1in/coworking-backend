package com.coworking.admin.audit.service;

import com.coworking.admin.audit.dto.AuditLogRequest;
import com.coworking.admin.audit.dto.AuditLogResponse;
import com.coworking.admin.audit.enums.AuditAction;
import com.coworking.admin.audit.model.AuditLog;
import com.coworking.admin.audit.repository.AuditLogRepository;
import com.coworking.admin.audit.repository.AuditLogSpecifications;
import com.coworking.admin.dto.AdminPageResponse;
import com.coworking.exception.NotFoundException;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private static final ZoneId EL_SALVADOR_ZONE =
            ZoneId.of("America/El_Salvador");

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void log(
            AuditAction action,
            String entityType,
            Long entityId
    ) {
        User admin = getAuthenticatedAdmin();

        AuditLog auditLog = new AuditLog();
        auditLog.setAdmin(admin);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);

        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPageResponse<AuditLogResponse> getAuditLogs(
            AuditLogRequest request,
            int page,
            int size
    ) {
        validateDateRange(request);

        Instant startDate = toStartOfDay(request.getStartDate());
        Instant endDate = toStartOfNextDay(request.getEndDate());

        String adminName = normalizeAdminName(request.getAdminName());

        Specification<AuditLog> specification = AuditLogSpecifications
                .hasAdminName(adminName)
                .and(AuditLogSpecifications.createdAtFrom(startDate))
                .and(AuditLogSpecifications.createdAtBefore(endDate));

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AuditLogResponse> logs = auditLogRepository
                .findAll(specification, pageable)
                .map(this::mapToResponse);

        return AdminPageResponse.<AuditLogResponse>builder()
                .content(logs.getContent())
                .page(logs.getNumber())
                .size(logs.getSize())
                .totalElements(logs.getTotalElements())
                .totalPages(logs.getTotalPages())
                .first(logs.isFirst())
                .last(logs.isLast())
                .build();
    }

    private User getAuthenticatedAdmin() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String currentEmail = authentication.getName();

        return userRepository.findByEmail(currentEmail)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Usuario autenticado no encontrado"
                        )
                );
    }

    private void validateDateRange(AuditLogRequest request) {
        if (request.getStartDate() == null ||
                request.getEndDate() == null) {
            return;
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin"
            );
        }
    }

    private Instant toStartOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }

        return date
                .atStartOfDay(EL_SALVADOR_ZONE)
                .toInstant();
    }

    private Instant toStartOfNextDay(LocalDate date) {
        if (date == null) {
            return null;
        }

        return date
                .plusDays(1)
                .atStartOfDay(EL_SALVADOR_ZONE)
                .toInstant();
    }

    private String normalizeAdminName(String adminName) {
        if (adminName == null || adminName.isBlank()) {
            return null;
        }

        return adminName.trim();
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getAdmin().getId(),
                auditLog.getAdmin().getUsername(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getCreatedAt()
        );
    }
}