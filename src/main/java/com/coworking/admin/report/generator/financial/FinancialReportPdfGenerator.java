package com.coworking.admin.report.generator.financial;

import com.coworking.admin.report.dto.financial.FinancialReportItem;
import com.coworking.admin.report.dto.financial.FinancialReportMetricResult;
import com.coworking.admin.report.dto.financial.FinancialReportResponse;
import com.coworking.payment.enums.PaymentStatus;
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
public class FinancialReportPdfGenerator {

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

    public byte[] generatePdf(
            FinancialReportResponse report
    ) {

        try {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document =
                    new Document(
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
            addPayments(document, report);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Error al generar el reporte financiero PDF",
                    e
            );
        }
    }

    private void addHeader(
            Document document,
            FinancialReportResponse report
    ) throws Exception {

        Paragraph company =
                new Paragraph(
                        COMPANY_NAME,
                        COMPANY_FONT
                );

        company.setAlignment(
                Element.ALIGN_CENTER
        );

        document.add(company);

        Paragraph title =
                new Paragraph(
                        "Reporte Financiero",
                        TITLE_FONT
                );

        title.setAlignment(
                Element.ALIGN_CENTER
        );

        document.add(title);

        Paragraph period =
                new Paragraph(
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

        document.add(
                new Paragraph(" ")
        );
    }

    private void addSummary(
            Document document,
            FinancialReportResponse report
    ) throws Exception {

        Paragraph sectionTitle =
                new Paragraph(
                        "Resumen del período",
                        SECTION_FONT
                );

        document.add(sectionTitle);

        document.add(
                new Paragraph(" ")
        );

        PdfPTable summaryTable =
                new PdfPTable(2);

        summaryTable.setWidthPercentage(100);

        summaryTable.setWidths(
                new float[]{3f, 1f}
        );

        for (var entry :
                report.metrics().entrySet()) {

            PdfPCell nameCell =
                    new PdfPCell(
                            new Phrase(
                                    getMetricLabel(
                                            entry.getKey()
                                    ),
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
                                    formatMetricValue(
                                            entry.getKey(),
                                            entry.getValue()
                                    ),
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

        document.add(
                new Paragraph(" ")
        );
    }

    private void addPayments(
            Document document,
            FinancialReportResponse report
    ) throws Exception {

        Paragraph sectionTitle =
                new Paragraph(
                        "Detalle de pagos",
                        SECTION_FONT
                );

        document.add(sectionTitle);

        document.add(
                new Paragraph(" ")
        );

        PdfPTable table =
                new PdfPTable(8);

        table.setWidthPercentage(100);

        table.setWidths(
                new float[]{
                        0.65f,
                        1.35f,
                        1.15f,
                        1.25f,
                        1.25f,
                        0.85f,
                        0.85f,
                        1.25f
                }
        );

        addHeaderCell(
                table,
                "ID Reserva"
        );

        addHeaderCell(
                table,
                "Usuario"
        );

        addHeaderCell(
                table,
                "Sala"
        );

        addHeaderCell(
                table,
                "Inicio"
        );

        addHeaderCell(
                table,
                "Fin"
        );

        addHeaderCell(
                table,
                "Monto"
        );

        addHeaderCell(
                table,
                "Estado"
        );

        addHeaderCell(
                table,
                "Fecha Pago"
        );

        for (FinancialReportItem item :
                report.reservations()) {

            addCell(
                    table,
                    String.valueOf(
                            item.reservationId()
                    ),
                    Element.ALIGN_CENTER
            );

            addCell(
                    table,
                    item.username(),
                    Element.ALIGN_LEFT
            );

            addCell(
                    table,
                    item.roomName(),
                    Element.ALIGN_LEFT
            );

            addCell(
                    table,
                    formatDateTime(
                            item.startAt()
                    ),
                    Element.ALIGN_CENTER
            );

            addCell(
                    table,
                    formatDateTime(
                            item.endAt()
                    ),
                    Element.ALIGN_CENTER
            );

            addCell(
                    table,
                    formatCurrency(
                            item.amount()
                    ),
                    Element.ALIGN_RIGHT
            );

            addCell(
                    table,
                    formatPaymentStatus(
                            item.paymentStatus()
                    ),
                    Element.ALIGN_CENTER
            );

            addCell(
                    table,
                    formatDateTime(
                            item.paidAt()
                    ),
                    Element.ALIGN_CENTER
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

        cell.setHorizontalAlignment(
                alignment
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(4);

        table.addCell(cell);
    }

    private String getMetricLabel(
            com.coworking.admin.report.enums.financial.FinancialReportMetric metric
    ) {

        return switch (metric) {

            case TOTAL_REVENUE ->
                    "Ingresos totales";

            case TOTAL_RESERVATIONS ->
                    "Cantidad de reservas";

            case AVERAGE_RESERVATION ->
                    "Promedio por reserva";

            case SUCCESSFUL_PAYMENTS ->
                    "Pagos exitosos";
        };
    }

    private String formatMetricValue(
            com.coworking.admin.report.enums.financial.FinancialReportMetric metric,
            Object value
    ) {

        return switch (metric) {

            case TOTAL_REVENUE,
                 AVERAGE_RESERVATION ->
                    formatCurrency(
                            (BigDecimal) value
                    );

            case TOTAL_RESERVATIONS,
                 SUCCESSFUL_PAYMENTS ->
                    String.valueOf(value);
        };
    }

    private String formatDate(
            java.time.LocalDate date
    ) {

        return date.format(
                DATE_FORMATTER
        );
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

    private String formatPaymentStatus(
            PaymentStatus status
    ) {

        return switch (status) {

            case SUCCEEDED -> "Exitoso";

            case PENDING -> "Pendiente";

            case FAILED -> "Fallido";
        };
    }
}