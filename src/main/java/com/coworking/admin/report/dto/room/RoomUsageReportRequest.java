package com.coworking.admin.report.dto.room;

import com.coworking.admin.report.enums.room.RoomUsageReportMetric;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record RoomUsageReportRequest(
        @NotNull(message = "startDate es obligatorio")
        LocalDate startDate,

        @NotNull(message = "endDate es obligatorio")
        LocalDate endDate,

        @NotEmpty(message = "Debe seleccionar al menos una métrica")
        List<RoomUsageReportMetric> metrics
) {
        @AssertTrue(message = "La fecha de fin debe ser igual o posterior a la fecha de inicio")
        public boolean isDateRangeValid() {

                if (startDate == null || endDate == null) {
                        return true;
                }

                return !endDate.isBefore(startDate);
        }
}
