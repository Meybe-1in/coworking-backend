package com.coworking.admin.audit.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequest {

    private String adminName;

    private LocalDate startDate;

    private LocalDate endDate;

    @AssertTrue(message = "La fecha de inicio no puede ser posterior a la fecha de fin")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }

        return !startDate.isAfter(endDate);
    }
}
