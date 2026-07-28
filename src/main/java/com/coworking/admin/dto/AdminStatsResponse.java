package com.coworking.admin.dto;

import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.room.repository.RoomRepository;
import com.coworking.user.repository.UserRepository;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AdminStatsResponse(
        long totalReservations,
        long activeReservations,
        long pendingReservations,
        long cancelledReservations,
        long expiredReservations,

        long totalUsers,
        long activeUsers,
        long disabledUsers,

        long totalRooms,
        long availableRooms,
        long unavailableRooms,

        long todayReservations,
        long monthReservations,

        BigDecimal totalRevenue,
        BigDecimal monthlyRevenue
) {

}
