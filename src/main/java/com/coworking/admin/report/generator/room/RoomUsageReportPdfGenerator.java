package com.coworking.admin.report.generator.room;

import com.coworking.admin.report.dto.room.RoomUsageReportItem;
import com.coworking.admin.report.dto.room.RoomUsageReportMetricResult;
import com.coworking.admin.report.dto.room.RoomUsageReportResponse;
import com.coworking.admin.report.enums.room.RoomUsageReportMetric;

import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.PageSize;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class RoomUsageReportPdfGenerator {

    private static final String COMPANY_NAME = "COWORKING";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy",
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

    public byte[] generatePdf(
            RoomUsageReportResponse report
    ) {

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
            addRooms(document, report);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Error al generar el reporte PDF de uso de salas",
                    e
            );
        }
    }

    private void addHeader(
            Document document,
            RoomUsageReportResponse report
    ) throws Exception {

        Paragraph company = new Paragraph(
                COMPANY_NAME,
                COMPANY_FONT
        );

        company.setAlignment(
                Element.ALIGN_CENTER
        );

        document.add(company);

        Paragraph title = new Paragraph(
                "Reporte de Uso de Salas",
                TITLE_FONT
        );

        title.setAlignment(
                Element.ALIGN_CENTER
        );

        document.add(title);

        Paragraph period = new Paragraph(
                "Período: "
                        + formatDate(report.startDate())
                        + " - "
                        + formatDate(report.endDate()),
                PERIOD_FONT
        );

        period.setAlignment(
                Element.ALIGN_CENTER
        );

        document.add(period);

        document.add(new Paragraph(" "));
    }

    private void addSummary(
            Document document,
            RoomUsageReportResponse report
    ) throws Exception {

        Paragraph sectionTitle = new Paragraph(
                "Resumen del período",
                SECTION_FONT
        );

        document.add(sectionTitle);

        document.add(new Paragraph(" "));

        PdfPTable summaryTable =
                new PdfPTable(2);

        summaryTable.setWidthPercentage(100);

        summaryTable.setWidths(
                new float[]{3f, 1f}
        );

        for (RoomUsageReportMetricResult metric
                : report.metrics()) {

            PdfPCell nameCell =
                    new PdfPCell(
                            new Phrase(
                                    getMetricLabel(metric),
                                    NORMAL_FONT
                            )
                    );

            nameCell.setBorder(
                    Rectangle.NO_BORDER
            );

            nameCell.setPadding(5);

            PdfPCell valueCell =
                    new PdfPCell(
                            new Phrase(
                                    formatMetricValue(metric),
                                    FontFactory.getFont(
                                            FontFactory.HELVETICA_BOLD,
                                            9
                                    )
                            )
                    );

            valueCell.setBorder(
                    Rectangle.NO_BORDER
            );

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

    private void addRooms(
            Document document,
            RoomUsageReportResponse report
    ) throws Exception {

        Paragraph sectionTitle = new Paragraph(
                "Uso por sala",
                SECTION_FONT
        );

        document.add(sectionTitle);

        document.add(new Paragraph(" "));

        PdfPTable table =
                new PdfPTable(4);

        table.setWidthPercentage(100);

        /*
         * Sala tiene mayor espacio.
         * Las métricas numéricas utilizan
         * columnas más pequeñas.
         */
        table.setWidths(
                new float[]{
                        2.5f,
                        1.5f,
                        1.5f,
                        1.8f
                }
        );

        addHeaderCell(
                table,
                "Sala"
        );

        addHeaderCell(
                table,
                "Reservas"
        );

        addHeaderCell(
                table,
                "Horas"
        );

        addHeaderCell(
                table,
                "Ocupación"
        );

        for (RoomUsageReportItem room
                : report.rooms()) {

            addCell(
                    table,
                    room.roomName(),
                    Element.ALIGN_LEFT
            );

            addCell(
                    table,
                    String.valueOf(
                            room.totalReservations()
                    ),
                    Element.ALIGN_CENTER
            );

            addCell(
                    table,
                    formatHours(
                            room.reservedHours()
                    ),
                    Element.ALIGN_RIGHT
            );

            addCell(
                    table,
                    formatPercentage(
                            room.occupancyPercentage()
                    ),
                    Element.ALIGN_RIGHT
            );
        }

        document.add(table);
    }

    private void addHeaderCell(
            PdfPTable table,
            String text
    ) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                HEADER_FONT
                        )
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

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                TABLE_FONT
                        )
                );

        cell.setHorizontalAlignment(alignment);

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(4);

        table.addCell(cell);
    }

    private String getMetricLabel(
            RoomUsageReportMetricResult metric
    ) {

        return switch (metric.name()) {

            case OCCUPANCY_PERCENTAGE ->
                    "Porcentaje de ocupación";

            case TOTAL_RESERVATIONS ->
                    "Cantidad de reservas";

            case RESERVED_HOURS ->
                    "Horas reservadas";
        };
    }

    private String formatMetricValue(
            RoomUsageReportMetricResult metric
    ) {

        return switch (metric.name()) {

            case OCCUPANCY_PERCENTAGE ->
                    formatPercentage(
                            ((Number) metric.value())
                                    .doubleValue()
                    );

            case TOTAL_RESERVATIONS ->
                    String.valueOf(
                            ((Number) metric.value())
                                    .longValue()
                    );

            case RESERVED_HOURS ->
                    formatHours(
                            ((Number) metric.value())
                                    .doubleValue()
                    );
        };
    }

    private String formatDate(
            LocalDate date
    ) {

        return date.format(
                DATE_FORMATTER
        );
    }

    private String formatHours(
            double hours
    ) {

        return String.format(
                Locale.US,
                "%.2f h",
                hours
        );
    }

    private String formatPercentage(
            double percentage
    ) {

        return String.format(
                Locale.US,
                "%.2f%%",
                percentage
        );
    }
}