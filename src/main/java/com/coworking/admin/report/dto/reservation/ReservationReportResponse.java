package com.coworking.admin.report.dto.reservation;

import java.time.LocalDate;
import java.util.List;

public record ReservationReportResponse(
    LocalDate startDate,
    LocalDate endDate,
    List<ReservationReportMetricResult> metrics,
    List<ReservationReportItem> reservations

) {
}
