package com.coworking.admin.util;

import com.coworking.admin.enums.ChartPeriod;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;

public final class ChartDateUtils {

    private ChartDateUtils() {
    }

    public static Instant getStartDate(ChartPeriod period) {

        LocalDate today = LocalDate.now();

        return switch (period) {

            case WEEK -> today.minusDays(6)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

            case MONTH -> today.withDayOfMonth(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

            case YEAR -> today.withDayOfYear(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
        };
    }

    public static Instant getEndDate() {

        return LocalDate.now()
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
    }
}