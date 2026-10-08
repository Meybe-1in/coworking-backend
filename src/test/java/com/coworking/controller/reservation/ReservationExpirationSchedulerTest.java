package com.coworking.controller.reservation;

import com.coworking.admin.notification.enums.NotificationType;
import com.coworking.admin.notification.service.NotificationService;
import com.coworking.admin.settings.entity.SystemSettings;
import com.coworking.admin.settings.service.SystemSettingsService;
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
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReservationExpirationSchedulerTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SystemSettingsService systemSettingsService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReservationExpirationScheduler scheduler;

    @Test
    void shouldExpirePendingReservations() {

        // Given
        SystemSettings settings = new SystemSettings();

        settings.setOpeningTime(LocalTime.of(7, 0));
        settings.setClosingTime(LocalTime.of(20, 0));
        settings.setMaxReservationHours(8);
        settings.setPendingExpirationMinutes(15);
        settings.setInstitutionName("Coworking Platform");

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(
                Instant.now().minus(Duration.ofMinutes(20))
        );

        when(reservationRepository.findByStatusAndCreatedAtBefore(
                eq(ReservationStatus.PENDING),
                any(Instant.class)
        )).thenReturn(List.of(reservation));

        // When
        scheduler.expiredPendingReservations();

        // Then
        assertEquals(
                ReservationStatus.EXPIRED,
                reservation.getStatus()
        );

        verify(notificationService).createNotificationForAdmins(
                NotificationType.RESERVATION_EXPIRED,
                "Reserva expirada",
                "La reserva #1 ha expirado.",
                "Reservation",
                1L
        );
    }
}