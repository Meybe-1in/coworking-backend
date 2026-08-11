package com.coworking.admin.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RoomOccupancyResponse(
        String roomName,
        long reservationCount,
        BigDecimal reservedHours,
        BigDecimal occupancyPercentage
) {
}
