package com.coworking.reservation.repository;

import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserEmailOrderByCreatedAtDesc(String email);

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

    List<Reservation> findByStatusAndStartAtLessThanAndEndAtGreaterThan(
            ReservationStatus status,
            Instant end,
            Instant start
    );

    //overlapping
    boolean existsByRoomIdAndStartAtLessThanAndEndAtGreaterThan(
            Long roomId,
            Instant endAt,
            Instant startAt

    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT r FROM Reservation r
                WHERE r.room.id = :roomId
                AND r.startAt < :endAt
                AND r.endAt > :startAt
            """)
    List<Reservation> findOverlappingForUpdate(
            @Param("roomId") Long roomId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );


    List<Reservation> findByStatusAndCreatedAtBefore(ReservationStatus status, Instant createdAt);

    //admin metricts

    long countByStatus( ReservationStatus status);
}
