package com.coworking.reservation.scheduler;

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

    @Scheduled(fixedRate = 60000) //un minuto
    @Transactional
    public void expiredPendingReservations(){

        Instant limit = Instant.now().minus(Duration.ofMinutes(15));

        List<Reservation> expiredReservations =
                reservationRepository
                        .findByStatusAndCreatedAtBefore(
                                ReservationStatus.PENDING,
                                limit
                        );

        for (Reservation reservation : expiredReservations){
            reservation.setStatus(ReservationStatus.EXPIRED);
            log.info(
                    "Reservación {} expirada automáticamente",
                    reservation.getId()
            );
        }
    }
}
