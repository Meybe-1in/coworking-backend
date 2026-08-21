package com.coworking.generator.financial;

import com.coworking.admin.report.dto.financial.FinancialReportItem;
import com.coworking.admin.report.dto.financial.FinancialReportResponse;
import com.coworking.admin.report.enums.financial.FinancialReportMetric;
import com.coworking.admin.report.generator.financial.FinancialReportPdfGenerator;
import com.coworking.payment.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinancialReportPdfGeneratorTest {

    private final FinancialReportPdfGenerator generator =
            new FinancialReportPdfGenerator();

    @Test
    void shouldGenerateFinancialPdf() {

        FinancialReportResponse report =
                createReport();

        byte[] result =
                generator.generatePdf(report);

        assertNotNull(result);

        assertTrue(
                result.length > 100
        );

        String header =
                new String(
                        result,
                        0,
                        Math.min(5, result.length),
                        StandardCharsets.ISO_8859_1
                );

        assertEquals(
                "%PDF-",
                header
        );
    }

    @Test
    void shouldGeneratePdfWithoutReservations() {

        FinancialReportResponse report =
                FinancialReportResponse.builder()
                        .startDate(
                                LocalDate.of(2026, 8, 1)
                        )
                        .endDate(
                                LocalDate.of(2026, 8, 20)
                        )
                        .metrics(
                                createMetrics()
                        )
                        .reservations(
                                List.of()
                        )
                        .build();

        byte[] result =
                generator.generatePdf(report);

        assertNotNull(result);

        assertTrue(
                result.length > 100
        );

        String header =
                new String(
                        result,
                        0,
                        5,
                        StandardCharsets.ISO_8859_1
                );

        assertEquals(
                "%PDF-",
                header
        );
    }

    private FinancialReportResponse createReport() {

        return FinancialReportResponse.builder()
                .startDate(
                        LocalDate.of(2026, 8, 1)
                )
                .endDate(
                        LocalDate.of(2026, 8, 20)
                )
                .metrics(
                        createMetrics()
                )
                .reservations(
                        List.of(
                                createItem()
                        )
                )
                .build();
    }

    private Map<FinancialReportMetric, Object> createMetrics() {

        Map<FinancialReportMetric, Object> metrics =
                new EnumMap<>(
                        FinancialReportMetric.class
                );

        metrics.put(
                FinancialReportMetric.TOTAL_REVENUE,
                new BigDecimal("300.00")
        );

        metrics.put(
                FinancialReportMetric.TOTAL_RESERVATIONS,
                3L
        );

        metrics.put(
                FinancialReportMetric.AVERAGE_RESERVATION,
                new BigDecimal("100.00")
        );

        metrics.put(
                FinancialReportMetric.SUCCESSFUL_PAYMENTS,
                3L
        );

        return metrics;
    }

    private FinancialReportItem createItem() {

        return FinancialReportItem.builder()
                .reservationId(1L)
                .username("dayana")
                .roomName("Sala Ejecutiva")
                .startAt(
                        Instant.parse(
                                "2026-08-10T18:00:00Z"
                        )
                )
                .endAt(
                        Instant.parse(
                                "2026-08-10T20:00:00Z"
                        )
                )
                .amount(
                        new BigDecimal("300.00")
                )
                .paymentStatus(
                        PaymentStatus.SUCCEEDED
                )
                .paidAt(
                        Instant.parse(
                                "2026-08-10T18:00:00Z"
                        )
                )
                .build();
    }
}