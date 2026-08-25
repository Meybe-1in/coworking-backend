package com.coworking.service.reports;

import com.coworking.admin.report.dto.room.RoomUsageReportRequest;
import com.coworking.admin.report.dto.room.RoomUsageReportResponse;
import com.coworking.admin.report.enums.room.RoomUsageReportMetric;
import com.coworking.admin.report.generator.room.RoomUsageReportCsvGenerator;
import com.coworking.admin.report.generator.room.RoomUsageReportPdfGenerator;
import com.coworking.admin.report.service.RoomReportServiceImpl;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.room.model.Room;
import com.coworking.room.repository.RoomRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomUsageReportServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomUsageReportPdfGenerator roomUsageReportPdfGenerator;

    @Mock
    private RoomUsageReportCsvGenerator roomUsageReportCsvGenerator;

    @Mock
    private Reservation reservation;

    @Mock
    private Room room;

    private RoomReportServiceImpl service;

    private final LocalDate START_DATE =
            LocalDate.of(2026, 7, 1);

    private final LocalDate END_DATE =
            LocalDate.of(2026, 7, 31);


    @BeforeEach
    void setUp() {

        service = new RoomReportServiceImpl(
                reservationRepository,
                roomRepository,
                roomUsageReportPdfGenerator,
                roomUsageReportCsvGenerator
        );
    }


    // =========================================================
    // GET ROOM USAGE REPORT
    // =========================================================

    @Test
    void shouldGetRoomUsageReportWithAllMetrics() {

        RoomUsageReportRequest request =
                new RoomUsageReportRequest(
                        START_DATE,
                        END_DATE,
                        List.of(
                                RoomUsageReportMetric.OCCUPANCY_PERCENTAGE,
                                RoomUsageReportMetric.TOTAL_RESERVATIONS,
                                RoomUsageReportMetric.RESERVED_HOURS
                        )
                );

        when(
                reservationRepository
                        .findPaidReservationsForRoomUsageReport(
                                eq(ReservationStatus.PAID),
                                any(Instant.class),
                                any(Instant.class)
                        )
        ).thenReturn(
                List.of(reservation)
        );

        when(roomRepository.findAll())
                .thenReturn(List.of(room));

        when(room.getId())
                .thenReturn(1L);

        when(room.getName())
                .thenReturn("Sala A");

        when(reservation.getRoom())
                .thenReturn(room);

        when(reservation.getStartAt())
                .thenReturn(
                        Instant.parse(
                                "2026-07-10T14:00:00Z"
                        )
                );

        when(reservation.getEndAt())
                .thenReturn(
                        Instant.parse(
                                "2026-07-10T17:00:00Z"
                        )
                );

        RoomUsageReportResponse response =
                service.getRoomUsageReport(request);

        assertNotNull(response);

        assertEquals(
                START_DATE,
                response.startDate()
        );

        assertEquals(
                END_DATE,
                response.endDate()
        );

        assertEquals(
                3,
                response.metrics().size()
        );

        assertEquals(
                1,
                response.rooms().size()
        );

        assertEquals(
                "Sala A",
                response.rooms()
                        .getFirst()
                        .roomName()
        );

        assertEquals(
                1,
                response.rooms()
                        .getFirst()
                        .totalReservations()
        );

        assertEquals(
                3.0,
                response.rooms()
                        .getFirst()
                        .reservedHours()
        );

        verify(reservationRepository)
                .findPaidReservationsForRoomUsageReport(
                        eq(ReservationStatus.PAID),
                        any(Instant.class),
                        any(Instant.class)
                );
    }


    // =========================================================
    // ONLY TOTAL RESERVATIONS
    // =========================================================

    @Test
    void shouldCalculateTotalReservations() {

        RoomUsageReportRequest request =
                new RoomUsageReportRequest(
                        START_DATE,
                        END_DATE,
                        List.of(
                                RoomUsageReportMetric.TOTAL_RESERVATIONS
                        )
                );

        when(
                reservationRepository
                        .findPaidReservationsForRoomUsageReport(
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(
                List.of(
                        reservation,
                        reservation
                )
        );

        when(roomRepository.findAll())
                .thenReturn(List.of(room));

        when(room.getId())
                .thenReturn(1L);

        when(room.getName())
                .thenReturn("Sala A");

        when(reservation.getRoom())
                .thenReturn(room);

        when(reservation.getStartAt())
                .thenReturn(
                        Instant.parse(
                                "2026-07-10T14:00:00Z"
                        )
                );

        when(reservation.getEndAt())
                .thenReturn(
                        Instant.parse(
                                "2026-07-10T17:00:00Z"
                        )
                );

        RoomUsageReportResponse response =
                service.getRoomUsageReport(request);

        assertEquals(
                2L,
                ((Number) response.metrics()
                        .getFirst()
                        .value())
                        .longValue()
        );
    }


    // =========================================================
    // RESERVED HOURS
    // =========================================================

    @Test
    void shouldCalculateReservedHours() {

        RoomUsageReportRequest request =
                new RoomUsageReportRequest(
                        START_DATE,
                        END_DATE,
                        List.of(
                                RoomUsageReportMetric.RESERVED_HOURS
                        )
                );

        when(
                reservationRepository
                        .findPaidReservationsForRoomUsageReport(
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(List.of(reservation));

        when(roomRepository.findAll())
                .thenReturn(List.of(room));

        when(room.getId())
                .thenReturn(1L);

        when(room.getName())
                .thenReturn("Sala A");

        when(reservation.getRoom())
                .thenReturn(room);

        when(reservation.getStartAt())
                .thenReturn(
                        Instant.parse(
                                "2026-07-10T14:00:00Z"
                        )
                );

        when(reservation.getEndAt())
                .thenReturn(
                        Instant.parse(
                                "2026-07-10T18:00:00Z"
                        )
                );

        RoomUsageReportResponse response =
                service.getRoomUsageReport(request);

        assertEquals(
                4.0,
                ((Number) response.metrics()
                        .getFirst()
                        .value())
                        .doubleValue()
        );
    }


    // =========================================================
    // NO RESERVATIONS
    // =========================================================

    @Test
    void shouldReturnZeroUsageWhenThereAreNoReservations() {

        RoomUsageReportRequest request =
                new RoomUsageReportRequest(
                        START_DATE,
                        END_DATE,
                        List.of(
                                RoomUsageReportMetric.OCCUPANCY_PERCENTAGE,
                                RoomUsageReportMetric.TOTAL_RESERVATIONS,
                                RoomUsageReportMetric.RESERVED_HOURS
                        )
                );

        when(
                reservationRepository
                        .findPaidReservationsForRoomUsageReport(
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(List.of());

        when(roomRepository.findAll())
                .thenReturn(List.of(room));

        when(room.getId())
                .thenReturn(1L);

        when(room.getName())
                .thenReturn("Sala A");

        RoomUsageReportResponse response =
                service.getRoomUsageReport(request);

        assertNotNull(response);

        assertEquals(
                0L,
                response.rooms()
                        .getFirst()
                        .totalReservations()
        );

        assertEquals(
                0.0,
                response.rooms()
                        .getFirst()
                        .reservedHours()
        );

        assertEquals(
                0.0,
                response.rooms()
                        .getFirst()
                        .occupancyPercentage()
        );
    }


    // =========================================================
    // GENERATE PDF
    // =========================================================

    @Test
    void shouldGenerateRoomUsageReportPdf() {

        RoomUsageReportRequest request =
                new RoomUsageReportRequest(
                        START_DATE,
                        END_DATE,
                        List.of(
                                RoomUsageReportMetric.OCCUPANCY_PERCENTAGE
                        )
                );

        when(
                reservationRepository
                        .findPaidReservationsForRoomUsageReport(
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(List.of());

        when(roomRepository.findAll())
                .thenReturn(List.of());

        RoomUsageReportResponse report =
                service.getRoomUsageReport(request);

        byte[] expectedPdf =
                "PDF".getBytes();

        when(
                roomUsageReportPdfGenerator.generatePdf(report)
        ).thenReturn(expectedPdf);

        byte[] result =
                service.generateRoomUsageReportPdf(request);

        assertArrayEquals(
                expectedPdf,
                result
        );

        verify(
                roomUsageReportPdfGenerator
        ).generatePdf(
                any(RoomUsageReportResponse.class)
        );
    }


    // =========================================================
    // GENERATE CSV
    // =========================================================

    @Test
    void shouldGenerateRoomUsageReportCsv() {

        RoomUsageReportRequest request =
                new RoomUsageReportRequest(
                        START_DATE,
                        END_DATE,
                        List.of(
                                RoomUsageReportMetric.OCCUPANCY_PERCENTAGE
                        )
                );

        when(
                reservationRepository
                        .findPaidReservationsForRoomUsageReport(
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(List.of());

        when(roomRepository.findAll())
                .thenReturn(List.of());

        RoomUsageReportResponse report =
                service.getRoomUsageReport(request);

        byte[] expectedCsv =
                "COWORKING".getBytes();

        when(
                roomUsageReportCsvGenerator.generateCsv(report)
        ).thenReturn(expectedCsv);

        byte[] result =
                service.generateRoomUsageReportCsv(request);

        assertArrayEquals(
                expectedCsv,
                result
        );

        verify(
                roomUsageReportCsvGenerator
        ).generateCsv(
                any(RoomUsageReportResponse.class)
        );
    }
}