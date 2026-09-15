package com.coworking.admin.settings.dto;

import java.time.LocalTime;

public record PublicReservationSettingsResponse(
        LocalTime openingTime,
        LocalTime closingTime,
        Integer maxReservationHours,
        Integer pendingExpirationMinutes
) {
}
