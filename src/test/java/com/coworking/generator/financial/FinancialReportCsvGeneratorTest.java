package com.coworking.generator.financial;

import com.coworking.admin.report.dto.financial.FinancialReportItem;
import com.coworking.admin.report.dto.financial.FinancialReportResponse;
import com.coworking.admin.report.enums.financial.FinancialReportMetric;
import com.coworking.admin.report.generator.financial.FinancialReportCsvGenerator;
import com.coworking.payment.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinancialReportCsvGeneratorTest {

    private final FinancialReportCsvGenerator generator =
            new FinancialReportCsvGenerator();

    @Test
    void shouldGenerateFinancialCsv() {

        FinancialReportResponse report =
                createReport();

        byte[] result =
                generator.generateCsv(report);

        assertNotNull(result);
        assertTrue(result.length > 0);

        String csv =
                new String(
                        result,
                        java.nio.charset.StandardCharsets.UTF_8
                );

        assertTrue(
                csv.contains("COWORKING")
        );

        assertTrue(
                csv.contains("Reporte Financiero")
        );

        assertTrue(
                csv.contains("Período: 01/08/2026 - 20/08/2026")
        );

        assertTrue(
                csv.contains("Resumen del período")
        );

        assertTrue(
                csv.contains("Ingresos totales,300.00")
        );

        assertTrue(
                csv.contains("Cantidad de reservas,3")
        );

        assertTrue(
                csv.contains("Promedio por reserva,100.00")
        );

        assertTrue(
                csv.contains("Pagos exitosos,3")
        );

        assertTrue(
                csv.contains(
                        "ID Reserva,Usuario,Sala,Inicio,Fin,Monto,Estado Pago,Fecha Pago"
                )
        );

        assertTrue(
                csv.contains("1")
        );

        assertTrue(
                csv.contains("dayana")
        );

        assertTrue(
                csv.contains("Sala Ejecutiva")
        );

        assertTrue(
                csv.contains("300.00")
        );

        assertTrue(
                csv.contains("Exitoso")
        );
    }

    @Test
    void shouldFormatDatesUsingElSalvadorTimezone() {

        FinancialReportResponse report =
                createReport();

        byte[] result =
                generator.generateCsv(report);

        String csv =
                new String(
                        result,
                        java.nio.charset.StandardCharsets.UTF_8
                );

        assertTrue(
                csv.contains("10/08/2026")
        );

        assertTrue(
                csv.contains("12:00")
        );
    }

    @Test
    void shouldGenerateCsvWithoutReservations() {

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
                generator.generateCsv(report);

        assertNotNull(result);

        String csv =
                new String(
                        result,
                        java.nio.charset.StandardCharsets.UTF_8
                );

        assertTrue(
                csv.contains("Detalle de pagos")
        );

        assertTrue(
                csv.contains("ID Reserva,Usuario,Sala")
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