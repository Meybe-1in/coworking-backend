package com.coworking.admin.report.dto.reservation;

import com.coworking.admin.report.enums.reservation.ReservationReportMetric;

import java.time.LocalDate;
import java.util.List;

public record ReservationReportRequest (
        LocalDate startDate,
        LocalDate endDate,
        List<ReservationReportMetric> metrics
){
}
