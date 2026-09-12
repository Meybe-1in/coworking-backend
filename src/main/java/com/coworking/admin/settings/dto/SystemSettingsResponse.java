package com.coworking.admin.settings.dto;

import java.time.Instant;
import java.time.LocalTime;

public record SystemSettingsResponse(

        LocalTime openingTime,
        LocalTime closingTime,
        Integer maxReservationHours,
        Integer pendingExpirationMinutes,
        String institutionName,
        String institutionEmail,
        String institutionPhone,
        String institutionAddress,
        Instant updatedAt
) {
}
