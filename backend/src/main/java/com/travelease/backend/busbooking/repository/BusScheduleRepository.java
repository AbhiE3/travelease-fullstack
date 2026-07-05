package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.BusSchedule;
import com.travelease.backend.busbooking.entity.enums.BusStatus;
import com.travelease.backend.busbooking.entity.enums.RouteStatus;
import com.travelease.backend.busbooking.entity.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BusScheduleRepository extends JpaRepository<BusSchedule, Long>, JpaSpecificationExecutor<BusSchedule> {

    @Query("SELECT s FROM BusSchedule s JOIN s.route r JOIN s.bus b " +
           "WHERE LOWER(r.source) = LOWER(:source) " +
           "AND LOWER(r.destination) = LOWER(:destination) " +
           "AND s.travelDate = :travelDate " +
           "AND s.status <> :cancelledStatus " +
           "AND b.status = :activeBusStatus " +
           "AND r.status = :activeRouteStatus " +
           "ORDER BY s.departureTime ASC")
    List<BusSchedule> findBySourceDestinationAndDate(
            @Param("source") String source,
            @Param("destination") String destination,
            @Param("travelDate") LocalDate travelDate,
            @Param("cancelledStatus") ScheduleStatus cancelledStatus,
            @Param("activeBusStatus") BusStatus activeBusStatus,
            @Param("activeRouteStatus") RouteStatus activeRouteStatus);

    List<BusSchedule> findByBusId(Long busId);

    List<BusSchedule> findByTravelDate(LocalDate date);

    @Query("SELECT COUNT(s) FROM BusSchedule s WHERE s.status = 'SCHEDULED'")
    Long countActiveSchedules();
}
