package com.coworking.admin.report.dto.financial;

import com.coworking.payment.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record FinancialReportItem(
        Long reservationId,

        String username,

        String roomName,

        Instant startAt,

        Instant endAt,

        BigDecimal amount,

        PaymentStatus paymentStatus,

        Instant paidAt
) {
}
