package com.coworking.admin.report.dto.room;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record RoomUsageReportResponse(
        LocalDate startDate,

        LocalDate endDate,

        List<RoomUsageReportMetricResult> metrics,

        List<RoomUsageReportItem> rooms
) {
}
