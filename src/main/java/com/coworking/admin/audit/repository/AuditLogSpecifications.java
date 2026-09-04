package com.coworking.admin.audit.repository;

import com.coworking.admin.audit.model.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> hasAdminName(String adminName) {
        return (root, query, criteriaBuilder) -> {

            if (adminName == null || adminName.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.like(
                    criteriaBuilder.lower(
                            root.get("admin").get("username")
                    ),
                    "%" + adminName.trim().toLowerCase() + "%"
            );
        };
    }

    public static Specification<AuditLog> createdAtFrom(Instant startDate) {
        return (root, query, criteriaBuilder) -> {

            if (startDate == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"),
                    startDate
            );
        };
    }

    public static Specification<AuditLog> createdAtBefore(Instant endDate) {
        return (root, query, criteriaBuilder) -> {

            if (endDate == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.lessThan(
                    root.get("createdAt"),
                    endDate
            );
        };
    }
}