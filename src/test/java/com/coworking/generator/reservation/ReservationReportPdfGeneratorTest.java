package com.coworking.generator.reservation;

import com.coworking.admin.report.dto.reservation.ReservationReportItem;
import com.coworking.admin.report.dto.reservation.ReservationReportMetricResult;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.admin.report.enums.reservation.ReservationReportMetric;
import com.coworking.admin.report.generator.reservation.ReservationReportPdfGenerator;
import com.coworking.reservation.enums.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReservationReportPdfGeneratorTest {

    private ReservationReportPdfGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ReservationReportPdfGenerator();
    }

    @Test
    void shouldGeneratePdfWithReportInformation() throws Exception {

        ReservationReportResponse report =
                createReport();

        byte[] pdf =
                generator.generatePdf(report);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        PdfReader reader = new PdfReader(pdf);

        assertTrue(reader.getNumberOfPages() > 0);

        PdfTextExtractor extractor = new PdfTextExtractor(reader);

        String text = extractor.getTextFromPage(1, false);

        assertTrue(
                text.contains("COWORKING")
        );

        assertTrue(
                text.contains("Reporte de Reservas")
        );

        assertTrue(
                text.contains("01/07/2026")
        );

        assertTrue(
                text.contains("31/07/2026")
        );

        assertTrue(
                text.contains("Total de reservas")
        );

        assertTrue(
                text.contains("Reservas pagadas")
        );

        assertTrue(
                text.contains("usuario_prueba")
        );

        assertTrue(
                text.contains("Sala A")
        );

        assertTrue(
                text.contains("$25.00")
        );

        reader.close();
    }

    @Test
    void shouldGeneratePdfWithOnlySelectedMetrics() throws Exception {

        ReservationReportResponse report =
                new ReservationReportResponse(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        List.of(
                                new ReservationReportMetricResult(
                                        ReservationReportMetric.TOTAL_RESERVAS,
                                        1
                                )
                        ),
                        List.of()
                );

        byte[] pdf =
                generator.generatePdf(report);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        PdfReader reader = new PdfReader(pdf);

        PdfTextExtractor extractor = new PdfTextExtractor(reader);

        String text = extractor.getTextFromPage(1, false);

        assertTrue(
                text.contains("Total de reservas")
        );

        assertFalse(
                text.contains("Reservas pagadas")
        );

        assertFalse(
                text.contains("Ingresos totales")
        );

        reader.close();
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
