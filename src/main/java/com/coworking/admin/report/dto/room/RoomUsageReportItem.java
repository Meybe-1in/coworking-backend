package com.coworking.admin.report.dto.room;

import lombok.Builder;

@Builder
public record RoomUsageReportItem(
        String roomName,

        long totalReservations,

        double reservedHours,

        double occupancyPercentage
) {
}
