package com.coworking.service.admin;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.ChartPointResponse;
import com.coworking.admin.dto.RoomOccupancyResponse;
import com.coworking.admin.enums.ChartPeriod;
import com.coworking.admin.service.AdminDashboardServiceImpl;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.role.repository.RoleRepository;
import com.coworking.room.repository.RoomRepository;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class AdminDashboardServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    // Estadísticas del dashboard
    @Test
    void shouldReturnAdminStats() {

        // Arrange
        when(reservationRepository.count())
                .thenReturn(10L);

        when(reservationRepository.countByStatus(ReservationStatus.PAID))
                .thenReturn(5L);

        when(reservationRepository.countByStatus(ReservationStatus.PENDING))
                .thenReturn(2L);

        when(reservationRepository.countByStatus(ReservationStatus.CANCELLED))
                .thenReturn(1L);

        when(reservationRepository.countByStatus(ReservationStatus.EXPIRED))
                .thenReturn(2L);

        when(userRepository.count())
                .thenReturn(25L);

        when(userRepository.countByEnabledTrue())
                .thenReturn(20L);

        when(userRepository.countByEnabledFalse())
                .thenReturn(5L);

        when(roomRepository.count())
                .thenReturn(8L);

        when(roomRepository.countByAvailableTrue())
                .thenReturn(6L);

        when(roomRepository.countByAvailableFalse())
                .thenReturn(2L);

        when(reservationRepository.countByCreatedAtBetween(any(), any()))
                .thenReturn(4L)
                .thenReturn(18L);

        when(paymentRepository.getTotalRevenue())
                .thenReturn(BigDecimal.valueOf(1000));

        when(paymentRepository.getMonthlyRevenue())
                .thenReturn(BigDecimal.valueOf(300));

        // Act
        AdminStatsResponse response = adminDashboardService.getStats();

        // Assert
        assertEquals(10L, response.totalReservations());
        assertEquals(5L, response.activeReservations());
        assertEquals(2L, response.pendingReservations());
        assertEquals(1L, response.cancelledReservations());
        assertEquals(2L, response.expiredReservations());

        assertEquals(25L, response.totalUsers());
        assertEquals(20L, response.activeUsers());
        assertEquals(5L, response.disabledUsers());

        assertEquals(8L, response.totalRooms());
        assertEquals(6L, response.availableRooms());
        assertEquals(2L, response.unavailableRooms());

        assertEquals(4L, response.todayReservations());
        assertEquals(18L, response.monthReservations());

        assertEquals(
                BigDecimal.valueOf(1000),
                response.totalRevenue()
        );

        assertEquals(
                BigDecimal.valueOf(300),
                response.monthlyRevenue()
        );

        verify(reservationRepository).count();

        verify(reservationRepository).countByStatus(ReservationStatus.PAID);
        verify(reservationRepository).countByStatus(ReservationStatus.PENDING);
        verify(reservationRepository).countByStatus(ReservationStatus.CANCELLED);
        verify(reservationRepository).countByStatus(ReservationStatus.EXPIRED);

        verify(userRepository).count();
        verify(userRepository).countByEnabledTrue();
        verify(userRepository).countByEnabledFalse();

        verify(roomRepository).count();
        verify(roomRepository).countByAvailableTrue();
        verify(roomRepository).countByAvailableFalse();

        verify(reservationRepository, times(2))
                .countByCreatedAtBetween(any(), any());

        verify(paymentRepository).getTotalRevenue();
        verify(paymentRepository).getMonthlyRevenue();
    }

    // Agrupación diaria para WEEK/MONTH
    @Test
    void shouldGroupReservationsByDayForShortPeriods() {

        Reservation firstReservation = new Reservation();
        firstReservation.setCreatedAt(
                LocalDate.of(2026, 7, 20)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );

        Reservation secondReservation = new Reservation();
        secondReservation.setCreatedAt(
                LocalDate.of(2026, 7, 20)
                        .atTime(10, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );

        Reservation thirdReservation = new Reservation();
        thirdReservation.setCreatedAt(
                LocalDate.of(2026, 7, 21)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );

        when(reservationRepository
                .findByCreatedAtBetweenOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(
                        firstReservation,
                        secondReservation,
                        thirdReservation
                ));

        List<ChartPointResponse> result =
                adminDashboardService.getReservationsChart(
                        ChartPeriod.WEEK
                );

        assertEquals(2, result.size());

        assertEquals("2026-07-20", result.get(0).getPeriod());
        assertEquals(BigDecimal.valueOf(2), result.get(0).getTotal());

        assertEquals("2026-07-21", result.get(1).getPeriod());
        assertEquals(BigDecimal.valueOf(1), result.get(1).getTotal());

        verify(reservationRepository)
                .findByCreatedAtBetweenOrderByCreatedAtAsc(
                        any(),
                        any()
                );

        verify(reservationRepository, never())
                .countReservationsByMonthCurrentYear();
    }

    //Incluir meses sin reservas con valor 0.
    @Test
    void shouldReturnTwelveMonthsForYearChartIncludingEmptyMonths() {

        when(reservationRepository.countReservationsByMonthCurrentYear())
                .thenReturn(List.of());

        List<ChartPointResponse> result =
                adminDashboardService.getReservationsChart(
                        ChartPeriod.YEAR
                );

        assertEquals(12, result.size());

        assertEquals("Enero", result.getFirst().getPeriod());
        assertEquals(BigDecimal.ZERO, result.getFirst().getTotal());

        assertEquals("Diciembre", result.get(11).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(11).getTotal());

        verify(reservationRepository)
                .countReservationsByMonthCurrentYear();

        verify(reservationRepository, never())
                .findByCreatedAtBetweenOrderByCreatedAtAsc(
                        any(),
                        any()
                );
    }

    //Agrupar correctamente por mes
    @Test
    void shouldReturnMonthlyTotalsFromRepository() {

        when(reservationRepository.countReservationsByMonthCurrentYear())
                .thenReturn(List.of(
                        new Object[]{1, 5L},
                        new Object[]{2, 8L},
                        new Object[]{7, 3L}
                ));

        List<ChartPointResponse> result =
                adminDashboardService.getReservationsChart(
                        ChartPeriod.YEAR
                );

        assertEquals(12, result.size());

        assertEquals("Enero", result.get(0).getPeriod());
        assertEquals(BigDecimal.valueOf(5), result.get(0).getTotal());

        assertEquals("Febrero", result.get(1).getPeriod());
        assertEquals(BigDecimal.valueOf(8), result.get(1).getTotal());

        assertEquals("Marzo", result.get(2).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(2).getTotal());

        assertEquals("Julio", result.get(6).getPeriod());
        assertEquals(BigDecimal.valueOf(3), result.get(6).getTotal());

        verify(reservationRepository)
                .countReservationsByMonthCurrentYear();
    }

    //Ingresos por dia
    @Test
    void shouldGroupRevenueByDayForShortPeriods() {

        when(paymentRepository.getRevenueGroupedByDay(any(), any()))
                .thenReturn(List.of(
                        new Object[]{"2026-07-25", new BigDecimal("150.00")},
                        new Object[]{"2026-07-27", new BigDecimal("300.00")}
                ));

        List<ChartPointResponse> result = adminDashboardService.getRevenueChart(ChartPeriod.WEEK);

        assertEquals(8, result.size());
        assertEquals("2026-07-25", result.get(0).getPeriod());
        assertEquals(new BigDecimal("150.00"), result.get(0).getTotal());
        assertEquals("2026-07-26", result.get(1).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(1).getTotal());

        verify(paymentRepository).getRevenueGroupedByDay(any(), any());
    }

    //Ingresos anual
    @Test
    void shouldReturnTwelveMonthsForRevenueYearChartIncludingEmptyMonths() {

        when(paymentRepository.getRevenueGroupedByMonthCurrentYear())
                .thenReturn(List.of(
                        new Object[]{1, new BigDecimal("1200.50")},
                        new Object[]{7, new BigDecimal("500.00")}
                ));

        List<ChartPointResponse> result = adminDashboardService.getRevenueChart(ChartPeriod.YEAR);

        assertEquals(12, result.size());
        assertEquals("Enero", result.get(0).getPeriod());
        assertEquals(new BigDecimal("1200.50"), result.get(0).getTotal());
        assertEquals("Febrero", result.get(1).getPeriod());
        assertEquals(BigDecimal.ZERO, result.get(1).getTotal());
        assertEquals("Julio", result.get(6).getPeriod());
        assertEquals(new BigDecimal("500.00"), result.get(6).getTotal());

        verify(paymentRepository).getRevenueGroupedByMonthCurrentYear();
    }

    @Test
    void shouldReturnRoomOccupancyRanking() {

        Object[] firstRoom =
                new Object[]{"Sala A", 20L, 100.0
                };


        Object[] secondRoom =
                new Object[]{"Sala B", 10L, 50.0
                };

        when(roomRepository.getRoomOccupancy())
                .thenReturn(
                        List.of(firstRoom, secondRoom)
                );


        List<RoomOccupancyResponse> result = adminDashboardService.getRoomOccupancy();

        assertEquals(2, result.size());
        assertEquals("Sala A", result.getFirst().roomName());
        assertEquals(20, result.getFirst().reservationCount());
        assertEquals(BigDecimal.valueOf(100.0), result.getFirst().reservedHours());

        assertTrue(
                result.getFirst()
                        .occupancyPercentage()
                        .compareTo(BigDecimal.ZERO) > 0
        );

        verify(roomRepository).getRoomOccupancy();
    }


}
