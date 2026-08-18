package com.coworking.service.reports;

import com.coworking.admin.report.dto.reservation.ReservationReportItem;
import com.coworking.admin.report.dto.reservation.ReservationReportMetricResult;
import com.coworking.admin.report.dto.reservation.ReservationReportRequest;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.admin.report.enums.reservation.ReservationReportMetric;
import com.coworking.admin.report.generator.reservation.ReservationReportCsvGenerator;
import com.coworking.admin.report.generator.reservation.ReservationReportPdfGenerator;
import com.coworking.admin.report.service.AdminReportServiceImpl;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.room.model.Room;
import com.coworking.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationReportPdfGenerator reservationReportPdfGenerator;

    @Mock
    private ReservationReportCsvGenerator reservationReportCsvGenerator;

    private AdminReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminReportServiceImpl(
                reservationRepository,
                reservationReportPdfGenerator,
                reservationReportCsvGenerator
        );
    }

    @Test
    void shouldGenerateReservationReportWithSelectedMetrics() {

        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 10);

        Instant reservationStart =
                Instant.parse("2026-07-08T13:00:00Z");

        Instant reservationEnd =
                Instant.parse("2026-07-08T15:00:00Z");

        Reservation paidReservation = createReservation(
                1L,
                reservationStart,
                reservationEnd,
                new BigDecimal("20.00"),
                ReservationStatus.PAID
        );

        Reservation cancelledReservation = createReservation(
                2L,
                Instant.parse("2026-07-09T13:00:00Z"),
                Instant.parse("2026-07-09T14:00:00Z"),
                new BigDecimal("10.00"),
                ReservationStatus.CANCELLED
        );

        when(reservationRepository.findReservationsOverlappingPeriod(
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(
                List.of(
                        paidReservation,
                        cancelledReservation
                )
        );

        ReservationReportRequest request =
                new ReservationReportRequest(
                        startDate,
                        endDate,
                        List.of(
                                ReservationReportMetric.TOTAL_RESERVAS,
                                ReservationReportMetric.HORAS_RESERVADAS,
                                ReservationReportMetric.TOTAL_REVENUE,
                                ReservationReportMetric.PAID
                        )
                );

        ReservationReportResponse response =
                service.getReservationReport(request);

        assertNotNull(response);

        assertEquals(startDate, response.startDate());
        assertEquals(endDate, response.endDate());

        assertEquals(4, response.metrics().size());
        assertEquals(2, response.reservations().size());

        ReservationReportMetricResult totalReservations =
                response.metrics().get(0);

        ReservationReportMetricResult reservedHours =
                response.metrics().get(1);

        ReservationReportMetricResult totalRevenue =
                response.metrics().get(2);

        ReservationReportMetricResult paid =
                response.metrics().get(3);

        assertEquals(
                ReservationReportMetric.TOTAL_RESERVAS,
                totalReservations.name()
        );

        assertEquals(
                2,
                ((Number) totalReservations.value()).intValue()
        );

        assertEquals(
                ReservationReportMetric.HORAS_RESERVADAS,
                reservedHours.name()
        );

        assertEquals(
                3.0,
                ((Number) reservedHours.value()).doubleValue(),
                0.01
        );

        assertEquals(
                ReservationReportMetric.TOTAL_REVENUE,
                totalRevenue.name()
        );

        assertEquals(
                new BigDecimal("20.00"),
                totalRevenue.value()
        );

        assertEquals(
                ReservationReportMetric.PAID,
                paid.name()
        );

        assertEquals(
                1,
                ((Number) paid.value()).intValue()
        );

        verify(reservationRepository)
                .findReservationsOverlappingPeriod(
                        any(Instant.class),
                        any(Instant.class)
                );
    }

    @Test
    void shouldReturnOnlySelectedMetrics() {

        Reservation reservation = createReservation(
                1L,
                Instant.parse("2026-07-08T13:00:00Z"),
                Instant.parse("2026-07-08T15:00:00Z"),
                new BigDecimal("20.00"),
                ReservationStatus.PAID
        );

        when(reservationRepository.findReservationsOverlappingPeriod(
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(List.of(reservation));

        ReservationReportRequest request =
                new ReservationReportRequest(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        List.of(
                                ReservationReportMetric.TOTAL_RESERVAS,
                                ReservationReportMetric.PAID
                        )
                );

        ReservationReportResponse response =
                service.getReservationReport(request);

        assertEquals(2, response.metrics().size());

        assertEquals(
                ReservationReportMetric.TOTAL_RESERVAS,
                response.metrics().get(0).name()
        );

        assertEquals(
                ReservationReportMetric.PAID,
                response.metrics().get(1).name()
        );
    }

    @Test
    void shouldCalculateRevenueOnlyFromPaidReservations() {

        Reservation paid = createReservation(
                1L,
                Instant.parse("2026-07-08T13:00:00Z"),
                Instant.parse("2026-07-08T14:00:00Z"),
                new BigDecimal("25.00"),
                ReservationStatus.PAID
        );

        Reservation cancelled = createReservation(
                2L,
                Instant.parse("2026-07-08T15:00:00Z"),
                Instant.parse("2026-07-08T16:00:00Z"),
                new BigDecimal("50.00"),
                ReservationStatus.CANCELLED
        );

        Reservation expired = createReservation(
                3L,
                Instant.parse("2026-07-08T17:00:00Z"),
                Instant.parse("2026-07-08T18:00:00Z"),
                new BigDecimal("100.00"),
                ReservationStatus.EXPIRED
        );

        when(reservationRepository.findReservationsOverlappingPeriod(
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(
                List.of(paid, cancelled, expired)
        );

        ReservationReportRequest request =
                new ReservationReportRequest(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        List.of(
                                ReservationReportMetric.TOTAL_REVENUE
                        )
                );

        ReservationReportResponse response =
                service.getReservationReport(request);

        assertEquals(1, response.metrics().size());

        assertEquals(
                new BigDecimal("25.00"),
                response.metrics().get(0).value()
        );
    }

    @Test
    void shouldMapReservationDataToReportItem() {

        Reservation reservation = createReservation(
                10L,
                Instant.parse("2026-07-08T13:00:00Z"),
                Instant.parse("2026-07-08T14:00:00Z"),
                new BigDecimal("15.00"),
                ReservationStatus.PAID
        );

        when(reservationRepository.findReservationsOverlappingPeriod(
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(List.of(reservation));

        ReservationReportRequest request =
                new ReservationReportRequest(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        List.of(
                                ReservationReportMetric.TOTAL_RESERVAS
                        )
                );

        ReservationReportResponse response =
                service.getReservationReport(request);

        assertEquals(1, response.reservations().size());

        ReservationReportItem item =
                response.reservations().get(0);

        assertEquals(10L, item.id());
        assertEquals("usuario_prueba", item.username());
        assertEquals("Sala A", item.roomName());
        assertEquals(
                new BigDecimal("15.00"),
                item.price()
        );
        assertEquals(
                ReservationStatus.PAID,
                item.status()
        );
    }

    private Reservation createReservation(
            Long id,
            Instant startAt,
            Instant endAt,
            BigDecimal price,
            ReservationStatus status
    ) {

        User user = new User();
        user.setId(1L);
        user.setUsername("usuario_prueba");

        Room room = new Room();
        room.setId(1L);
        room.setName("Sala A");

        Reservation reservation = new Reservation();

        reservation.setId(id);
        reservation.setUser(user);
        reservation.setRoom(room);
        reservation.setStartAt(startAt);
        reservation.setEndAt(endAt);
        reservation.setPrice(price);
        reservation.setStatus(status);

        return reservation;
    }
}