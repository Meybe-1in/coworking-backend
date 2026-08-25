package com.coworking.admin.report.dto.room;

import com.coworking.admin.report.enums.room.RoomUsageReportMetric;
import lombok.Builder;

@Builder
public record RoomUsageReportMetricResult(
        RoomUsageReportMetric name,

        double value
) {
}
