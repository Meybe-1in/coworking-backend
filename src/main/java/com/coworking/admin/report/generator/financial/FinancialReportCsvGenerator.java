package com.coworking.admin.report.generator.financial;

import com.coworking.admin.report.dto.financial.FinancialReportItem;
import com.coworking.admin.report.dto.financial.FinancialReportMetricResult;
import com.coworking.admin.report.dto.financial.FinancialReportResponse;
import com.coworking.payment.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class FinancialReportCsvGenerator {

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

    private static final String NEW_LINE = "\r\n";

    public byte[] generateCsv(FinancialReportResponse report) {

        try {
            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            OutputStreamWriter writer =
                    new OutputStreamWriter(
                            outputStream,
                            StandardCharsets.UTF_8
                    );

            writeHeader(writer, report);
            writer.write(NEW_LINE);
            writeSummary(writer, report);
            writer.write(NEW_LINE);
            writePayments(writer, report);
            writer.flush();
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Error al generar el reporte financiero CSV",
                    e
            );
        }
    }

    private void writeHeader(
            OutputStreamWriter writer,
            FinancialReportResponse report
    ) throws IOException {

        writer.write(COMPANY_NAME);
        writer.write(NEW_LINE);

        writer.write("Reporte Financiero");
        writer.write(NEW_LINE);

        writer.write("Período: ");
        writer.write(formatDate(report.startDate()));
        writer.write(" - ");
        writer.write(formatDate(report.endDate()));
    }

    private void writeSummary(
            OutputStreamWriter writer,
            FinancialReportResponse report
    ) throws IOException {

        writer.write("Resumen del período");
        writer.write(NEW_LINE);

        for (FinancialReportMetricResult metric :
                report.metrics().entrySet().stream()
                        .map(entry ->
                                new FinancialReportMetricResult(
                                        entry.getKey(),
                                        entry.getValue()
                                )
                        )
                        .toList()) {

            writer.write(getMetricLabel(metric));
            writer.write(",");
            writer.write(formatMetricValue(metric));
            writer.write(NEW_LINE);
        }
    }

    private void writePayments(
            OutputStreamWriter writer,
            FinancialReportResponse report
    ) throws IOException {

        writer.write("Detalle de pagos");
        writer.write(NEW_LINE);

        writer.write(
                "ID Reserva,Usuario,Sala,Inicio,Fin,Monto,Estado Pago,Fecha Pago"
        );

        writer.write(NEW_LINE);

        for (FinancialReportItem item : report.reservations()) {

            writer.write(
                    escapeCsv(String.valueOf(item.reservationId()))
            );
            writer.write(",");

            writer.write(
                    escapeCsv(item.username())
            );
            writer.write(",");

            writer.write(
                    escapeCsv(item.roomName())
            );
            writer.write(",");

            writer.write(
                    formatDateTime(item.startAt())
            );
            writer.write(",");

            writer.write(
                    formatDateTime(item.endAt())
            );
            writer.write(",");

            writer.write(
                    formatCurrency(item.amount())
            );
            writer.write(",");

            writer.write(
                    formatPaymentStatus(item.paymentStatus())
            );
            writer.write(",");

            writer.write(
                    formatDateTime(item.paidAt())
            );

            writer.write(NEW_LINE);
        }
    }

    private String getMetricLabel(
            FinancialReportMetricResult metric
    ) {

        return switch (metric.name()) {

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
            FinancialReportMetricResult metric
    ) {

        return switch (metric.name()) {

            case TOTAL_REVENUE,
                 AVERAGE_RESERVATION ->
                    formatCurrency(
                            (BigDecimal) metric.value()
                    );

            case TOTAL_RESERVATIONS,
                 SUCCESSFUL_PAYMENTS ->
                    String.valueOf(metric.value());
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
                "%.2f",
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

    private String escapeCsv(String value) {

        if (value == null) {
            return "";
        }

        if (value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r")) {

            return "\"" +
                    value.replace("\"", "\"\"") +
                    "\"";
        }

        return value;
    }
}