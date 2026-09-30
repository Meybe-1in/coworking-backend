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

    //verificar si tiene reserva identica
    Optional<Reservation> findByUserIdAndRoomIdAndStartAtAndEndAtAndStatusIn(
            Long userId,
            Long roomId,
            Instant start,
            Instant end,
            List<ReservationStatus> statuses
    );

    List<Reservation> findByStatusAndStartAtLessThanAndEndAtGreaterThan(
            ReservationStatus status,
            Instant end,
            Instant start
    );

    // reservas que bloquean disponibilidad
    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.status IN :statuses
              AND r.startAt < :endAt
              AND r.endAt > :startAt
            """)
    List<Reservation> findActiveOverlappingReservations(
            @Param("statuses") List<ReservationStatus> statuses,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );

    // reservas de una sala que se cruzan con un intervalo
    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.room.id = :roomId
              AND r.status IN :statuses
              AND r.startAt < :endAt
              AND r.endAt > :startAt
            ORDER BY r.startAt ASC
            """)
    List<Reservation> findRoomOverlappingReservations(
            @Param("roomId") Long roomId,
            @Param("statuses") List<ReservationStatus> statuses,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );

    //overlapping con bloqueo para creación de reservas

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT r FROM Reservation r
                WHERE r.room.id = :roomId
                AND r.status IN :statuses
                AND r.startAt < :endAt
                AND r.endAt > :startAt
            """)
    List<Reservation> findOverlappingForUpdate(
            @Param("roomId") Long roomId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("statuses") List<ReservationStatus> statuses
    );


    List<Reservation> findByStatusAndCreatedAtBefore(ReservationStatus status, Instant createdAt);

    //admin metricts

    long countByStatus(ReservationStatus status);

    long countByCreatedAtBetween(Instant start, Instant end);

    List<Reservation> findByCreatedAtBetweenOrderByCreatedAtAsc(Instant start, Instant end);

    @Query("""
            SELECT
                MONTH(r.createdAt),
                COUNT(r)
            FROM Reservation r
            WHERE YEAR(r.createdAt)=YEAR(CURRENT_DATE)
            GROUP BY MONTH(r.createdAt)
            ORDER BY MONTH(r.createdAt)
            """)
    List<Object[]> countReservationsByMonthCurrentYear();

    List<Reservation> findTop8ByOrderByCreatedAtDesc();

    // admin report
    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.startAt < :endAt
              AND r.endAt > :startAt
            """)
    List<Reservation> findReservationsOverlappingPeriod(
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );


    @Query("""
            SELECT r
            FROM Reservation r
            JOIN FETCH r.room
            WHERE r.status = :status
              AND r.startAt < :endDateTime
              AND r.endAt > :startDateTime
            ORDER BY r.room.name ASC, r.startAt ASC
            """)
    List<Reservation> findPaidReservationsForRoomUsageReport(
            @Param("status") ReservationStatus status,
            @Param("startDateTime") Instant startDateTime,
            @Param("endDateTime") Instant endDateTime
    );
}
