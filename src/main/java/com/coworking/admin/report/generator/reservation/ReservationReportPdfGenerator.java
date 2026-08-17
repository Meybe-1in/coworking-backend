package com.coworking.admin.report.generator.reservation;

import com.coworking.admin.report.dto.reservation.ReservationReportItem;
import com.coworking.admin.report.dto.reservation.ReservationReportMetricResult;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.reservation.enums.ReservationStatus;

import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.PageSize;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class ReservationReportPdfGenerator {

    private static final String COMPANY_NAME = "COWORKING";

    private static final ZoneId ZONE_ID =
            ZoneId.of("America/El_Salvador");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy",
                    Locale.of("es", "SV")
            );

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm",
                    Locale.of("es", "SV")
            );

    private static final Font COMPANY_FONT =
            FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    11
            );

    private static final Font TITLE_FONT =
            FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    20
            );

    private static final Font PERIOD_FONT =
            FontFactory.getFont(
                    FontFactory.HELVETICA,
                    10
            );

    private static final Font SECTION_FONT =
            FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    12
            );

    private static final Font NORMAL_FONT =
            FontFactory.getFont(
                    FontFactory.HELVETICA,
                    9
            );

    private static final Font HEADER_FONT =
            FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    8
            );

    private static final Font TABLE_FONT =
            FontFactory.getFont(
                    FontFactory.HELVETICA,
                    8
            );

    public byte[] generatePdf(ReservationReportResponse report) {

        try {
            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document = new Document(
                    PageSize.A4,
                    36,
                    36,
                    36,
                    36
            );

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();

            addHeader(document, report);
            addSummary(document, report);
            addReservations(document, report);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error al generar el reporte PDF",
                    e
            );
        }
    }

    private void addHeader(
            Document document,
            ReservationReportResponse report
    ) throws Exception {

        Paragraph company = new Paragraph(
                COMPANY_NAME,
                COMPANY_FONT
        );

        company.setAlignment(Element.ALIGN_CENTER);
        document.add(company);

        Paragraph title = new Paragraph(
                "Reporte de Reservas",
                TITLE_FONT
        );

        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph period = new Paragraph(
                "Período: "
                        + formatDate(report.startDate())
                        + " - "
                        + formatDate(report.endDate()),
                PERIOD_FONT
        );

        period.setAlignment(Element.ALIGN_CENTER);
        document.add(period);

        document.add(new Paragraph(" "));
    }

    private void addSummary(
            Document document,
            ReservationReportResponse report
    ) throws Exception {

        Paragraph sectionTitle = new Paragraph(
                "Resumen del período",
                SECTION_FONT
        );

        document.add(sectionTitle);
        document.add(new Paragraph(" "));

        PdfPTable summaryTable = new PdfPTable(2);

        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{3f, 1f});

        for (ReservationReportMetricResult metric
                : report.metrics()) {

            PdfPCell nameCell = new PdfPCell(
                    new Phrase(
                            getMetricLabel(metric),
                            NORMAL_FONT
                    )
            );

            nameCell.setBorder(Rectangle.NO_BORDER);
            nameCell.setPadding(5);

            PdfPCell valueCell = new PdfPCell(
                    new Phrase(
                            formatMetricValue(metric),
                            FontFactory.getFont(
                                    FontFactory.HELVETICA_BOLD,
                                    9
                            )
                    )
            );

            valueCell.setBorder(Rectangle.NO_BORDER);
            valueCell.setHorizontalAlignment(
                    Element.ALIGN_RIGHT
            );
            valueCell.setPadding(5);

            summaryTable.addCell(nameCell);
            summaryTable.addCell(valueCell);
        }

        document.add(summaryTable);
        document.add(new Paragraph(" "));
    }

    private void addReservations(
            Document document,
            ReservationReportResponse report
    ) throws Exception {

        Paragraph sectionTitle = new Paragraph(
                "Detalle de reservas",
                SECTION_FONT
        );

        document.add(sectionTitle);
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(7);

        table.setWidthPercentage(100);

        /*
         * ID pequeño.
         * Usuario y Sala más amplios.
         * Fechas con espacio suficiente.
         */
        table.setWidths(new float[]{
                0.45f,
                1.65f,
                1.25f,
                1.55f,
                1.55f,
                0.85f,
                0.95f
        });

        addHeaderCell(table, "ID");
        addHeaderCell(table, "Usuario");
        addHeaderCell(table, "Sala");
        addHeaderCell(table, "Inicio");
        addHeaderCell(table, "Fin");
        addHeaderCell(table, "Precio");
        addHeaderCell(table, "Estado");

        for (ReservationReportItem reservation
                : report.reservations()) {

            addCell(
                    table,
                    String.valueOf(reservation.id()),
                    Element.ALIGN_CENTER
            );

            addCell(
                    table,
                    reservation.username(),
                    Element.ALIGN_LEFT
            );

            addCell(
                    table,
                    reservation.roomName(),
                    Element.ALIGN_LEFT
            );

            addCell(
                    table,
                    formatDateTime(reservation.startAt()),
                    Element.ALIGN_CENTER
            );

            addCell(
                    table,
                    formatDateTime(reservation.endAt()),
                    Element.ALIGN_CENTER
            );

            addCell(
                    table,
                    formatCurrency(reservation.price()),
                    Element.ALIGN_RIGHT
            );

            addCell(
                    table,
                    formatStatus(reservation.status()),
                    Element.ALIGN_CENTER
            );
        }

        document.add(table);
    }

    private void addHeaderCell(
            PdfPTable table,
            String text
    ) {

        PdfPCell cell = new PdfPCell(
                new Phrase(text, HEADER_FONT)
        );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(5);

        table.addCell(cell);
    }

    private void addCell(
            PdfPTable table,
            String text,
            int alignment
    ) {

        PdfPCell cell = new PdfPCell(
                new Phrase(text, TABLE_FONT)
        );

        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(4);

        table.addCell(cell);
    }

    private String getMetricLabel(
            ReservationReportMetricResult metric
    ) {

        return switch (metric.name()) {

            case TOTAL_RESERVAS -> "Total de reservas";

            case HORAS_RESERVADAS -> "Horas reservadas";

            case TOTAL_REVENUE -> "Ingresos pagados totales";

            case PAID -> "Reservas pagadas";
        };
    }

    private String formatMetricValue(
            ReservationReportMetricResult metric
    ) {

        return switch (metric.name()) {

            case TOTAL_RESERVAS,
                 PAID -> String.valueOf(metric.value());

            case HORAS_RESERVADAS -> String.format(
                    Locale.US,
                    "%.2f h",
                    ((Number) metric.value())
                            .doubleValue()
            );

            case TOTAL_REVENUE -> formatCurrency(
                    (BigDecimal) metric.value()
            );
        };
    }

    private String formatDate(
            java.time.LocalDate date
    ) {

        return date.format(DATE_FORMATTER);
    }

    private String formatDateTime(
            Instant instant
    ) {

        return instant
                .atZone(ZONE_ID)
                .format(DATE_TIME_FORMATTER);
    }

    private String formatCurrency(
            BigDecimal amount
    ) {

        return String.format(
                Locale.US,
                "$%.2f",
                amount
        );
    }

    private String formatStatus(
            ReservationStatus status
    ) {

        return switch (status) {

            case PAID -> "Pagada";

            case PENDING -> "Pendiente";

            case CANCELLED -> "Cancelada";

            case EXPIRED -> "Expirada";
        };
    }
}