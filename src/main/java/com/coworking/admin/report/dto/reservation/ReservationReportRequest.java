package com.coworking.admin.report.dto.reservation;

import com.coworking.admin.report.enums.reservation.ReservationReportMetric;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ReservationReportRequest(
        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate startDate,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate endDate,

        @NotEmpty(message = "Debe seleccionar al menos una métrica")
        List<ReservationReportMetric> metrics
) {
    @AssertTrue(message = "La fecha de fin debe ser igual o posterior a la fecha de inicio")
    public boolean isDateRangeValid() {

        if (startDate == null || endDate == null) {
            return true;
        }

        return !endDate.isBefore(startDate);
    }
}
