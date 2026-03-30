package com.coworking.reservation.repository;

import com.coworking.reservation.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    //verificar si se cruzan horarios
    List<Reservation> findByRoomIdAndStartAtLessThanAndEndAtGreaterThan(
            Long roomId,
            Instant end,
            Instant start
    );

   //verificar si tiene reserva identica
   Optional<Reservation> findByUserIdAndRoomIdAndStartAtAndEndAt(
           Long userId,
           Long roomId,
           Instant start,
           Instant end
   );

    List<Reservation> findByStartAtLessThanAndEndAtGreaterThan(
            Instant end,
            Instant start);

}
