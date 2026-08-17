package com.coworking.admin.report.generator.reservation;

import com.coworking.admin.report.dto.reservation.ReservationReportItem;
import com.coworking.admin.report.dto.reservation.ReservationReportMetricResult;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.reservation.enums.ReservationStatus;
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
public class ReservationReportCsvGenerator {

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

    public byte[] generateCsv(ReservationReportResponse report) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(
                    outputStream,
                    StandardCharsets.UTF_8
            );

            writeHeader(writer, report);
            writer.write(NEW_LINE);
            writeSummary(writer, report);
            writer.write(NEW_LINE);
            writeReservations(writer, report);

            writer.flush();
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Error al generar el reporte CSV",
                    e
            );
        }
    }

    private void writeHeader(
            OutputStreamWriter writer,
            ReservationReportResponse report
    ) throws IOException {

        writer.write(COMPANY_NAME);
        writer.write(NEW_LINE);
        writer.write("Reporte de Reservas");
        writer.write(NEW_LINE);
        writer.write("Período: ");
        writer.write(formatDate(report.startDate()));
        writer.write(" - ");
        writer.write(formatDate(report.endDate()));
    }

    private void writeSummary(
            OutputStreamWriter writer,
            ReservationReportResponse report
    ) throws IOException {

        writer.write("Resumen del período");
        writer.write(NEW_LINE);

        for (ReservationReportMetricResult metric : report.metrics()) {
            writer.write(getMetricLabel(metric));
            writer.write(",");
            writer.write(formatMetricValue(metric));
            writer.write(NEW_LINE);
        }
    }

    private void writeReservations(
            OutputStreamWriter writer,
            ReservationReportResponse report
    ) throws IOException {

        writer.write("Detalle de reservas");
        writer.write(NEW_LINE);

        writer.write("ID,Usuario,Sala,Inicio,Fin,Precio,Estado");
        writer.write(NEW_LINE);

        for (ReservationReportItem reservation : report.reservations()) {
            writer.write(escapeCsv(String.valueOf(reservation.id())));
            writer.write(",");
            writer.write(escapeCsv(reservation.username()));
            writer.write(",");
            writer.write(escapeCsv(reservation.roomName()));
            writer.write(",");
            writer.write(formatDateTime(reservation.startAt()));
            writer.write(",");
            writer.write(formatDateTime(reservation.endAt()));
            writer.write(",");
            writer.write(formatCurrency(reservation.price()));
            writer.write(",");
            writer.write(formatStatus(reservation.status()));
            writer.write(NEW_LINE);
        }
    }

    private String getMetricLabel(ReservationReportMetricResult metric) {
        return switch (metric.name()) {
            case TOTAL_RESERVAS -> "Total de reservas";
            case HORAS_RESERVADAS -> "Horas reservadas";
            case TOTAL_REVENUE -> "Ingresos pagados totales";
            case PAID -> "Reservas pagadas";
        };
    }

    private String formatMetricValue(ReservationReportMetricResult metric) {
        return switch (metric.name()) {
            case TOTAL_RESERVAS,
                 PAID -> String.valueOf(metric.value());
            case HORAS_RESERVADAS -> String.format(
                    Locale.US,
                    "%.2f h",
                    ((Number) metric.value()).doubleValue()
            );
            case TOTAL_REVENUE -> formatCurrency(
                    (BigDecimal) metric.value()
            );
        };
    }

    private String formatDate(java.time.LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private String formatDateTime(Instant instant) {
        return instant.atZone(ZONE_ID).format(DATE_TIME_FORMATTER);
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private String formatStatus(ReservationStatus status) {
        return switch (status) {
            case PAID -> "Pagada";
            case PENDING -> "Pendiente";
            case CANCELLED -> "Cancelada";
            case EXPIRED -> "Expirada";
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
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
