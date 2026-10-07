package com.coworking.reservation.scheduler;

import com.coworking.admin.notification.enums.NotificationType;
import com.coworking.admin.notification.service.NotificationService;
import com.coworking.admin.settings.entity.SystemSettings;
import com.coworking.admin.settings.service.SystemSettingsService;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpirationScheduler {
    private final ReservationRepository reservationRepository;
    private final SystemSettingsService systemSettingsService;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 60000) //un minuto
    @Transactional
    public void expiredPendingReservations() {

        SystemSettings settings =
                systemSettingsService.getCurrentSettings();

        Duration expirationDuration =
                Duration.ofMinutes(
                        settings.getPendingExpirationMinutes()
                );

        Instant limit =
                Instant.now().minus(expirationDuration);

        List<Reservation> expiredReservations =
                reservationRepository
                        .findByStatusAndCreatedAtBefore(
                                ReservationStatus.PENDING,
                                limit
                        );

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);

            notificationService.createNotificationForAdmins(
                    NotificationType.RESERVATION_EXPIRED,
                    "Reserva expirada",
                    "La reserva #" + reservation.getId() + " ha expirado.",
                    "Reservation",
                    reservation.getId()
            );

            log.info(
                    "Reservación {} expirada automáticamente",
                    reservation.getId()
            );
        }
    }
}
