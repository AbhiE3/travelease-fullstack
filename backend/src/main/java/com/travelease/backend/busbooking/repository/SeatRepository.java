package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.Seat;
import com.travelease.backend.busbooking.entity.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByBusId(Long busId);

    List<Seat> findByBusIdAndStatus(Long busId, SeatStatus status);

    @Query("SELECT s FROM Seat s WHERE s.bus.id = " +
           "(SELECT sc.bus.id FROM BusSchedule sc WHERE sc.id = :scheduleId) " +
           "AND s.status = 'AVAILABLE' " +
           "AND s.id NOT IN (" +
           "  SELECT bs.seat.id FROM BookingSeat bs " +
           "  WHERE bs.booking.schedule.id = :scheduleId " +
           "  AND bs.booking.status = 'CONFIRMED'" +
           ")")
    List<Seat> findAvailableSeatsForSchedule(@Param("scheduleId") Long scheduleId);
}
