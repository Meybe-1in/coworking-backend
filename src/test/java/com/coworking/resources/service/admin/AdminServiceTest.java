package com.coworking.resources.service.admin;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.service.AdminServiceImpl;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void shouldReturnAdminStats() {

        when(reservationRepository.count()).thenReturn(10L);

        when(reservationRepository.countByStatus(ReservationStatus.PAID))
                .thenReturn(5L);

        when(reservationRepository.countByStatus(ReservationStatus.PENDING))
                .thenReturn(2L);

        when(reservationRepository.countByStatus(ReservationStatus.CANCELLED))
                .thenReturn(1L);

        when(reservationRepository.countByStatus(ReservationStatus.EXPIRED))
                .thenReturn(2L);

        when(paymentRepository.getTotalRevenue())
                .thenReturn(BigDecimal.valueOf(1000));

        when(paymentRepository.getMonthlyRevenue())
                .thenReturn(BigDecimal.valueOf(300));

        AdminStatsResponse response = adminService.getStats();

        assertEquals(10L, response.totalReservations());
        assertEquals(5L, response.activeReservations());
        assertEquals(BigDecimal.valueOf(1000), response.totalRevenue());
    }

    // Cancel reservation

    @Test
    void shouldCancelReservation(){
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.CANCELLED);

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        adminService.cancelReservation(1L);

        assertEquals(
                ReservationStatus.CANCELLED,
                reservation.getStatus()
        );

        verify(reservationRepository).save(reservation);
    }
}
