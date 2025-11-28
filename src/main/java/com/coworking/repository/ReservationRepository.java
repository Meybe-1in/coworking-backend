package com.coworking.repository;

import com.coworking.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    //verificar si se cruzan horarios
    List<Reservation> findByRoomIdAndStartAtLessThanAndEndAtGreaterThan(
            Long roomId,
            LocalDateTime end,
            LocalDateTime start
    );

   //verificar si tiene reserva identica
   Optional<Reservation> findByUserIdAndRoomIdAndStartAtAndEndAt(
           Long userId,
           Long roomId,
           LocalDateTime start,
           LocalDateTime end
   );

    List<Reservation> findByStartAtLessThanAndEndAtGreaterThan(
            LocalDateTime end,
            LocalDateTime start);

}
