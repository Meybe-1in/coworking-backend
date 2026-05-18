package com.coworking.resources.controller.reservation;

import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.reservation.scheduler.ReservationExpirationScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReservationExpirationSchedulerTest {
    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationExpirationScheduler scheduler;

    @Test
    void shouldExpirePendingReservations() {

        Reservation reservation = new Reservation();

        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.PENDING);

        reservation.setCreatedAt(
                Instant.now().minus(Duration.ofMinutes(20))
        );

        when(reservationRepository
                .findByStatusAndCreatedBefore(
                        eq(ReservationStatus.PENDING),
                        any()
                ))
                .thenReturn(List.of(reservation));

        scheduler.expiredPendingReservations();

        assertEquals(
                ReservationStatus.EXPIRED,
                reservation.getStatus()
        );
    }
}
