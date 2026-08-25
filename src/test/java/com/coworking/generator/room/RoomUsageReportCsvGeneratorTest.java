package com.coworking.generator.room;

import com.coworking.admin.report.dto.room.RoomUsageReportItem;
import com.coworking.admin.report.dto.room.RoomUsageReportMetricResult;
import com.coworking.admin.report.dto.room.RoomUsageReportResponse;
import com.coworking.admin.report.enums.room.RoomUsageReportMetric;
import com.coworking.admin.report.generator.room.RoomUsageReportCsvGenerator;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomUsageReportCsvGeneratorTest {

    private final RoomUsageReportCsvGenerator generator =
            new RoomUsageReportCsvGenerator();


    @Test
    void shouldGenerateRoomUsageCsv() {

        RoomUsageReportResponse report =
                createReport();

        byte[] result =
                generator.generateCsv(report);

        assertNotNull(result);

        assertTrue(
                result.length > 0
        );

        String csv =
                new String(
                        result,
                        StandardCharsets.UTF_8
                );

        assertTrue(
                csv.contains("COWORKING")
        );

        assertTrue(
                csv.contains("Reporte de Uso de Salas")
        );

        assertTrue(
                csv.contains(
                        "Período: 01/07/2026 - 31/07/2026"
                )
        );

        assertTrue(
                csv.contains("Resumen del período")
        );

        assertTrue(
                csv.contains(
                        "Porcentaje de ocupación"
                )
        );

        assertTrue(
                csv.contains(
                        "Cantidad de reservas"
                )
        );

        assertTrue(
                csv.contains(
                        "Horas reservadas"
                )
        );

        assertTrue(
                csv.contains("Uso por sala")
        );

        assertTrue(
                csv.contains(
                        "Sala,Cantidad de reservas,Horas reservadas,Porcentaje de ocupación"
                )
        );

        assertTrue(
                csv.contains("Sala A")
        );

        assertTrue(
                csv.contains("10")
        );

        assertTrue(
                csv.contains("40.00")
        );

        assertTrue(
                csv.contains("83.33")
        );
    }


    @Test
    void shouldGenerateCsvWithoutRooms() {

        RoomUsageReportResponse report =
                RoomUsageReportResponse.builder()
                        .startDate(
                                LocalDate.of(2026, 7, 1)
                        )
                        .endDate(
                                LocalDate.of(2026, 7, 31)
                        )
                        .metrics(
                                createMetrics()
                        )
                        .rooms(
                                List.of()
                        )
                        .build();

        byte[] result =
                generator.generateCsv(report);

        assertNotNull(result);

        String csv =
                new String(
                        result,
                        StandardCharsets.UTF_8
                );

        assertTrue(
                csv.contains("Uso por sala")
        );

        assertTrue(
                csv.contains(
                        "Sala,Cantidad de reservas,Horas reservadas,Porcentaje de ocupación"
                )
        );
    }


    @Test
    void shouldGenerateCsvWithOnlySelectedMetric() {

        RoomUsageReportResponse report =
                RoomUsageReportResponse.builder()
                        .startDate(
                                LocalDate.of(2026, 7, 1)
                        )
                        .endDate(
                                LocalDate.of(2026, 7, 31)
                        )
                        .metrics(
                                List.of(
                                        RoomUsageReportMetricResult.builder()
                                                .name(
                                                        RoomUsageReportMetric
                                                                .OCCUPANCY_PERCENTAGE
                                                )
                                                .value(83.33)
                                                .build()
                                )
                        )
                        .rooms(
                                List.of()
                        )
                        .build();

        byte[] result =
                generator.generateCsv(report);

        String csv =
                new String(
                        result,
                        StandardCharsets.UTF_8
                );

        assertTrue(
                csv.contains(
                        "Porcentaje de ocupación"
                )
        );

        assertFalse(
                csv.contains(
                        "Cantidad de reservas,10"
                )
        );
    }


    private RoomUsageReportResponse createReport() {

        return RoomUsageReportResponse.builder()
                .startDate(
                        LocalDate.of(2026, 7, 1)
                )
                .endDate(
                        LocalDate.of(2026, 7, 31)
                )
                .metrics(
                        createMetrics()
                )
                .rooms(
                        List.of(
                                createRoomItem()
                        )
                )
                .build();
    }


    private List<RoomUsageReportMetricResult> createMetrics() {

        return List.of(

                RoomUsageReportMetricResult.builder()
                        .name(
                                RoomUsageReportMetric
                                        .OCCUPANCY_PERCENTAGE
                        )
                        .value(83.33)
                        .build(),

                RoomUsageReportMetricResult.builder()
                        .name(
                                RoomUsageReportMetric
                                        .TOTAL_RESERVATIONS
                        )
                        .value(10)
                        .build(),

                RoomUsageReportMetricResult.builder()
                        .name(
                                RoomUsageReportMetric
                                        .RESERVED_HOURS
                        )
                        .value(40.0)
                        .build()
        );
    }


    private RoomUsageReportItem createRoomItem() {

        return RoomUsageReportItem.builder()
                .roomName("Sala A")
                .totalReservations(10)
                .reservedHours(40.0)
                .occupancyPercentage(83.33)
                .build();
    }
}