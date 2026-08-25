package com.coworking.admin.report.generator.room;

import com.coworking.admin.report.dto.room.RoomUsageReportItem;
import com.coworking.admin.report.dto.room.RoomUsageReportMetricResult;
import com.coworking.admin.report.dto.room.RoomUsageReportResponse;
import com.coworking.admin.report.enums.room.RoomUsageReportMetric;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class RoomUsageReportCsvGenerator {

    private static final String COMPANY_NAME = "COWORKING";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy",
                    Locale.of("es", "SV")
            );

    private static final String NEW_LINE = "\r\n";

    public byte[] generateCsv(RoomUsageReportResponse report) {

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

            writeRooms(writer, report);

            writer.flush();

            return outputStream.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Error al generar el reporte CSV de uso de salas",
                    e
            );
        }
    }

    private void writeHeader(
            OutputStreamWriter writer,
            RoomUsageReportResponse report
    ) throws IOException {

        writer.write(COMPANY_NAME);
        writer.write(NEW_LINE);

        writer.write("Reporte de Uso de Salas");
        writer.write(NEW_LINE);

        writer.write("Período: ");
        writer.write(formatDate(report.startDate()));
        writer.write(" - ");
        writer.write(formatDate(report.endDate()));
    }

    private void writeSummary(
            OutputStreamWriter writer,
            RoomUsageReportResponse report
    ) throws IOException {

        writer.write("Resumen del período");
        writer.write(NEW_LINE);

        for (RoomUsageReportMetricResult metric :
                report.metrics()) {

            writer.write(
                    escapeCsv(getMetricLabel(metric.name()))
            );

            writer.write(",");

            writer.write(
                    escapeCsv(formatMetricValue(metric))
            );

            writer.write(NEW_LINE);
        }
    }

    private void writeRooms(
            OutputStreamWriter writer,
            RoomUsageReportResponse report
    ) throws IOException {

        writer.write("Uso por sala");
        writer.write(NEW_LINE);

        writer.write(
                "Sala,Cantidad de reservas,Horas reservadas,Porcentaje de ocupación"
        );

        writer.write(NEW_LINE);

        for (RoomUsageReportItem room : report.rooms()) {

            writer.write(
                    escapeCsv(room.roomName())
            );

            writer.write(",");

            writer.write(
                    String.valueOf(room.totalReservations())
            );

            writer.write(",");

            writer.write(
                    String.valueOf(room.reservedHours())
            );

            writer.write(",");

            writer.write(
                    String.valueOf(room.occupancyPercentage())
            );

            writer.write(NEW_LINE);
        }
    }

    private String getMetricLabel(
            RoomUsageReportMetric metric
    ) {

        return switch (metric) {

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
                    metric.value() + "%";

            case TOTAL_RESERVATIONS ->
                    String.valueOf(
                            ((Number) metric.value()).longValue()
                    );

            case RESERVED_HOURS ->
                    metric.value() + " h";
        };
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
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