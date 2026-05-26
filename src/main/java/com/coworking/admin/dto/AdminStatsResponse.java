package com.coworking.admin.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AdminStatsResponse(
        long totalReservations,
        long activeReservations,
        long pendingReservations,
        long cancelledReservations,
        long expiredReservations,

        BigDecimal totalRevenue,
        BigDecimal monthlyRevenue
) {

}
