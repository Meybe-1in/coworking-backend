package com.coworking.room.repository;

import com.coworking.room.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByCapacityOrderByCapacityAsc(Integer capacity);

    long countByAvailableTrue();

    long countByAvailableFalse();

    @Query(value = """
            SELECT
                r.name,
                COUNT(res.id),
                COALESCE(
                    SUM(
                        EXTRACT(EPOCH FROM (res.end_at - res.start_at))/3600
                    ),
                    0
                )
            FROM rooms r
            LEFT JOIN reservations res
                ON res.room_id = r.id
                AND res.status = 'PAID'
            GROUP BY r.id, r.name
            ORDER BY COUNT(res.id) DESC
            """,
            nativeQuery = true)
    List<Object[]> getRoomOccupancy();

}
