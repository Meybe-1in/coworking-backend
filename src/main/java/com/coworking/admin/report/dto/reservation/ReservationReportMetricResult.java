package com.coworking.admin.report.dto.reservation;

import com.coworking.admin.report.enums.reservation.ReservationReportMetric;

public record ReservationReportMetricResult(
        ReservationReportMetric name,
        Object value
) {
}
