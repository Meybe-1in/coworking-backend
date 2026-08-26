package com.coworking.generator.reservation;

import com.coworking.admin.report.dto.reservation.ReservationReportItem;
import com.coworking.admin.report.dto.reservation.ReservationReportMetricResult;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.admin.report.enums.reservation.ReservationReportMetric;
import com.coworking.admin.report.generator.reservation.ReservationReportCsvGenerator;
import com.coworking.reservation.enums.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReservationReportCsvGeneratorTest {

    private ReservationReportCsvGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ReservationReportCsvGenerator();
    }

    @Test
    void shouldGenerateCsvWithReportInformation() {

        ReservationReportResponse report =
                createReport();

        byte[] csv =
                generator.generateCsv(report);

        assertNotNull(csv);
        assertTrue(csv.length > 0);

        String content =
                new String(
                        csv,
                        StandardCharsets.UTF_8
                );

        assertTrue(
                content.contains("COWORKING")
        );

        assertTrue(
                content.contains("Reporte de Reservas")
        );

        assertTrue(
                content.contains("01/07/2026 - 31/07/2026")
        );

        assertTrue(
                content.contains("Total de reservas,1")
        );

        assertTrue(
                content.contains("Reservas pagadas,1")
        );

        assertTrue(
                content.contains(
                        "ID,Usuario,Sala,Inicio,Fin,Precio,Estado"
                )
        );

        assertTrue(
                content.contains("usuario_prueba")
        );

        assertTrue(
                content.contains("Sala A")
        );

        assertTrue(
                content.contains("25.00")
        );

        assertTrue(
                content.contains("Pagada")
        );
    }

    @Test
    void shouldIncludeOnlySelectedMetrics() {

        ReservationReportResponse report =
                new ReservationReportResponse(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        List.of(
                                new ReservationReportMetricResult(
                                        ReservationReportMetric.TOTAL_RESERVAS,
                                        5
                                )
                        ),
                        List.of()
                );

        byte[] csv =
                generator.generateCsv(report);

        String content =
                new String(
                        csv,
                        StandardCharsets.UTF_8
                );

        assertTrue(
                content.contains("Total de reservas,5")
        );

        assertFalse(
                content.contains("Reservas pagadas")
        );

        assertFalse(
                content.contains("Ingresos pagados totales")
        );

        assertFalse(
                content.contains("Horas reservadas")
        );
    }

    @Test
    void shouldEscapeCsvValuesContainingCommas() {

        ReservationReportItem reservation =
                new ReservationReportItem(
                        1L,
                        "usuario,prueba",
                        "Sala A",
                        Instant.parse(
                                "2026-07-08T13:00:00Z"
                        ),
                        Instant.parse(
                                "2026-07-08T14:00:00Z"
                        ),
                        new BigDecimal("10.00"),
                        ReservationStatus.PAID
                );

        ReservationReportResponse report =
                new ReservationReportResponse(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        List.of(),
                        List.of(reservation)
                );

        byte[] csv =
                generator.generateCsv(report);

        String content =
                new String(
                        csv,
                        StandardCharsets.UTF_8
                );

        assertTrue(
                content.contains("\"usuario,prueba\"")
        );
    }

    private ReservationReportResponse createReport() {

        ReservationReportMetricResult totalReservations =
                new ReservationReportMetricResult(
                        ReservationReportMetric.TOTAL_RESERVAS,
                        1
                );

        ReservationReportMetricResult paid =
                new ReservationReportMetricResult(
                        ReservationReportMetric.PAID,
                        1
                );

        ReservationReportItem reservation =
                new ReservationReportItem(
                        1L,
                        "usuario_prueba",
                        "Sala A",
                        Instant.parse(
                                "2026-07-08T13:00:00Z"
                        ),
                        Instant.parse(
                                "2026-07-08T14:00:00Z"
                        ),
                        new BigDecimal("25.00"),
                        ReservationStatus.PAID
                );

        return new ReservationReportResponse(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                List.of(
                        totalReservations,
                        paid
                ),
                List.of(reservation)
        );
    }
}
