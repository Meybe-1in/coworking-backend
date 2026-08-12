package com.coworking.admin.report.dto.reservation;

import com.coworking.reservation.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservationReportItem(
        Long id,
        String username,
        String roomName,
        Instant startAt,
        Instant endAt,
        BigDecimal price,
        ReservationStatus status
) {
}
