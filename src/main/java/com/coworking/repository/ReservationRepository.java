package com.coworking.repository;

import com.coworking.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByRoomIdAndStartAtLessThanEndAtGreaterThan(
            Long roomId, LocalDateTime end, LocalDateTime start
    );
}
