package com.coworking.service.reservation;

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
import static org.mockito.Mockito.*;

class ReservationExpirationSchedulerTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SystemSettingsService systemSettingsService;

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

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

    }

    @Test
    void shouldExpirePendingReservationsUsingConfiguredTime() {

        // Given
        settings.setPendingExpirationMinutes(15);

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.PENDING);

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
    }

    @Test
    void shouldUseDynamicPendingExpirationMinutes() {

        // Given
        settings.setPendingExpirationMinutes(30);

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

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

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        Reservation reservation1 = new Reservation();
        reservation1.setId(1L);
        reservation1.setStatus(ReservationStatus.PENDING);

        Reservation reservation2 = new Reservation();
        reservation2.setId(2L);
        reservation2.setStatus(ReservationStatus.PENDING);

        Reservation reservation3 = new Reservation();
        reservation3.setId(3L);
        reservation3.setStatus(ReservationStatus.PENDING);

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
    }

    @Test
    void shouldNotSaveReservationsBecauseTransactionPersistsChanges() {

        // Given
        settings.setPendingExpirationMinutes(15);

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.PENDING);

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
}