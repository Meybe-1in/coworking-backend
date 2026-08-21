package com.coworking.admin.report.service;

import com.coworking.admin.report.dto.financial.FinancialReportRequest;
import com.coworking.admin.report.dto.financial.FinancialReportResponse;
import com.coworking.admin.report.enums.financial.FinancialReportMetric;
import com.coworking.payment.enums.PaymentStatus;
import com.coworking.payment.model.Payment;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.model.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements FinancialReportService {

    private final PaymentRepository paymentRepository;

    private static final ZoneId ZONE_ID = ZoneId.of("America/El_Salvador");

    // FINANCIAL REPORT
    @Override
    public FinancialReportResponse getFinancialReport(FinancialReportRequest request) {
        validateDates(request);

        Instant start = toStartOfDay(request.startDate());
        Instant end = toStartOfDay(request.endDate().plusDays(1));

        Map<FinancialReportMetric, Object> metrics = new EnumMap<>(FinancialReportMetric.class);

        BigDecimal totalRevenue = null;
        Long totalReservations = null;

        for (FinancialReportMetric metric : request.metrics()) {
            switch (metric) {
                case TOTAL_REVENUE -> {
                    totalRevenue = paymentRepository.getRevenueByPeriod(
                            PaymentStatus.SUCCEEDED,
                            start,
                            end
                    );

                    metrics.put(
                            FinancialReportMetric.TOTAL_REVENUE, totalRevenue
                    );
                }

                case TOTAL_RESERVATIONS -> {
                    totalReservations =
                            paymentRepository
                                    .countReservationsByPaymentStatusAndPeriod(
                                            PaymentStatus.SUCCEEDED,
                                            start,
                                            end
                                    );

                    metrics.put(
                            FinancialReportMetric.TOTAL_RESERVATIONS,
                            totalReservations
                    );
                }
                case AVERAGE_RESERVATION -> {

                    if (totalRevenue == null) {
                        totalRevenue = paymentRepository.getRevenueByPeriod(
                                PaymentStatus.SUCCEEDED,
                                start,
                                end
                        );
                    }

                    if (totalReservations == null) {
                        totalReservations =
                                paymentRepository
                                        .countReservationsByPaymentStatusAndPeriod(
                                                PaymentStatus.SUCCEEDED,
                                                start,
                                                end
                                        );
                    }

                    BigDecimal average = calculateAverage(
                            totalRevenue,
                            totalReservations
                    );

                    metrics.put(
                            FinancialReportMetric.AVERAGE_RESERVATION,
                            average
                    );
                }

                case SUCCESSFUL_PAYMENTS -> {
                    long successfulPayment =
                            paymentRepository
                                    .countPaymentsByStatusAndPeriod(
                                            PaymentStatus.SUCCEEDED,
                                            start,
                                            end
                                    );

                    metrics.put(
                            FinancialReportMetric.SUCCESSFUL_PAYMENTS,
                            successfulPayment
                    );
                }
            }
        }

        return FinancialReportResponse.builder()
                .startDate(request.startDate())
                .endDate(request.endDate())
                .metrics(metrics)
                .build();

    }

    private BigDecimal calculateAverage(BigDecimal totalRevenue, Long totalReservations) {
        if (totalReservations == 0L) return BigDecimal.ZERO;

        return totalRevenue.divide(
                BigDecimal.valueOf(totalReservations),
                2,
                RoundingMode.HALF_UP
        );
    }

    private Instant toStartOfDay(LocalDate date) {
        return date
                .atStartOfDay(ZONE_ID)
                .toInstant();
    }

    private void validateDates(FinancialReportRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin"
            );
        }
    }
}