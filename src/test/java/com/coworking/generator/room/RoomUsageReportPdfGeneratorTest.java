package com.coworking.generator.room;

import com.coworking.admin.report.dto.room.RoomUsageReportItem;
import com.coworking.admin.report.dto.room.RoomUsageReportMetricResult;
import com.coworking.admin.report.dto.room.RoomUsageReportResponse;
import com.coworking.admin.report.enums.room.RoomUsageReportMetric;
import com.coworking.admin.report.generator.room.RoomUsageReportPdfGenerator;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomUsageReportPdfGeneratorTest {

    private final RoomUsageReportPdfGenerator generator =
            new RoomUsageReportPdfGenerator();


    @Test
    void shouldGenerateRoomUsagePdf() {

        RoomUsageReportResponse report =
                createReport();

        byte[] result =
                generator.generatePdf(report);

        assertNotNull(result);

        assertTrue(
                result.length > 100
        );

        String header =
                new String(
                        result,
                        0,
                        5,
                        StandardCharsets.ISO_8859_1
                );

        assertEquals(
                "%PDF-",
                header
        );
    }


    @Test
    void shouldGeneratePdfWithoutRooms() {

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
                generator.generatePdf(report);

        assertNotNull(result);

        assertTrue(
                result.length > 100
        );

        String header =
                new String(
                        result,
                        0,
                        5,
                        StandardCharsets.ISO_8859_1
                );

        assertEquals(
                "%PDF-",
                header
        );
    }


    @Test
    void shouldGeneratePdfWithOnlySelectedMetric() {

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
                                List.of(
                                        createRoomItem()
                                )
                        )
                        .build();

        byte[] result =
                generator.generatePdf(report);

        assertNotNull(result);

        assertTrue(
                result.length > 100
        );

        String header =
                new String(
                        result,
                        0,
                        5,
                        StandardCharsets.ISO_8859_1
                );

        assertEquals(
                "%PDF-",
                header
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
