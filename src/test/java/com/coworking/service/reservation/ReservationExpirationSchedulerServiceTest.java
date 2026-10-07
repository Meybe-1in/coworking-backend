package com.coworking.service.reservation;

import com.coworking.admin.notification.enums.NotificationType;
import com.coworking.admin.notification.service.NotificationService;
import com.coworking.admin.settings.entity.SystemSettings;
import com.coworking.admin.settings.service.SystemSettingsService;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.reservation.scheduler.ReservationExpirationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationExpirationSchedulerServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SystemSettingsService systemSettingsService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReservationExpirationScheduler scheduler;

    private SystemSettings settings;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        settings = new SystemSettings();
        settings.setOpeningTime(LocalTime.of(7, 0));
        settings.setClosingTime(LocalTime.of(20, 0));
        settings.setMaxReservationHours(8);
        settings.setPendingExpirationMinutes(15);
        settings.setInstitutionName("Coworking Platform");

        scheduler = new ReservationExpirationScheduler(
                reservationRepository,
                systemSettingsService,
                notificationService
        );

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);
    }

    @Test
    void shouldExpirePendingReservationsUsingConfiguredTime() {

        // Given
        settings.setPendingExpirationMinutes(15);

        Reservation reservation = createPendingReservation(1L);

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

        verify(systemSettingsService)
                .getCurrentSettings();

        verify(reservationRepository)
                .findByStatusAndCreatedAtBefore(
                        eq(ReservationStatus.PENDING),
                        any(Instant.class)
                );

        verify(notificationService)
                .createNotificationForAdmins(
                        NotificationType.RESERVATION_EXPIRED,
                        "Reserva expirada",
                        "La reserva #1 ha expirado.",
                        "Reservation",
                        1L
                );
    }

    @Test
    void shouldUseDynamicPendingExpirationMinutes() {

        // Given
        settings.setPendingExpirationMinutes(30);

        when(reservationRepository.findByStatusAndCreatedAtBefore(
                eq(ReservationStatus.PENDING),
                any(Instant.class)
        )).thenReturn(List.of());

        // When
        scheduler.expiredPendingReservations();

        // Then
        verify(systemSettingsService)
                .getCurrentSettings();

        verify(reservationRepository)
                .findByStatusAndCreatedAtBefore(
                        eq(ReservationStatus.PENDING),
                        any(Instant.class)
                );
    }

    @Test
    void shouldExpireMultiplePendingReservations() {

        // Given
        settings.setPendingExpirationMinutes(20);

        Reservation reservation1 = createPendingReservation(1L);
        Reservation reservation2 = createPendingReservation(2L);
        Reservation reservation3 = createPendingReservation(3L);

        when(reservationRepository.findByStatusAndCreatedAtBefore(
                eq(ReservationStatus.PENDING),
                any(Instant.class)
        )).thenReturn(
                List.of(
                        reservation1,
                        reservation2,
                        reservation3
                )
        );

        // When
        scheduler.expiredPendingReservations();

        // Then
        assertEquals(
                ReservationStatus.EXPIRED,
                reservation1.getStatus()
        );

        assertEquals(
                ReservationStatus.EXPIRED,
                reservation2.getStatus()
        );

        assertEquals(
                ReservationStatus.EXPIRED,
                reservation3.getStatus()
        );

        verify(reservationRepository)
                .findByStatusAndCreatedAtBefore(
                        eq(ReservationStatus.PENDING),
                        any(Instant.class)
                );

        verify(notificationService)
                .createNotificationForAdmins(
                        NotificationType.RESERVATION_EXPIRED,
                        "Reserva expirada",
                        "La reserva #1 ha expirado.",
                        "Reservation",
                        1L
                );

        verify(notificationService)
                .createNotificationForAdmins(
                        NotificationType.RESERVATION_EXPIRED,
                        "Reserva expirada",
                        "La reserva #2 ha expirado.",
                        "Reservation",
                        2L
                );

        verify(notificationService)
                .createNotificationForAdmins(
                        NotificationType.RESERVATION_EXPIRED,
                        "Reserva expirada",
                        "La reserva #3 ha expirado.",
                        "Reservation",
                        3L
                );
    }

    @Test
    void shouldNotSaveReservationsBecauseTransactionPersistsChanges() {

        // Given
        settings.setPendingExpirationMinutes(15);

        Reservation reservation = createPendingReservation(1L);

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

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }

    @Test
    void shouldCreateNotificationWhenReservationExpires() {

        // Given
        Reservation reservation = createPendingReservation(1L);

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

        verify(notificationService)
                .createNotificationForAdmins(
                        NotificationType.RESERVATION_EXPIRED,
                        "Reserva expirada",
                        "La reserva #1 ha expirado.",
                        "Reservation",
                        1L
                );
    }

    private Reservation createPendingReservation(Long id) {

        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStatus(ReservationStatus.PENDING);

        return reservation;
    }
}