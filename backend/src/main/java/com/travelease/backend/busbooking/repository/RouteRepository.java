package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.Route;
import com.travelease.backend.busbooking.entity.enums.RouteStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findBySourceIgnoreCaseAndDestinationIgnoreCase(String source, String destination);

    List<Route> findBySourceContainingIgnoreCase(String source);

    List<Route> findByStatus(RouteStatus status);

    long countByStatus(RouteStatus status);

    @Query("SELECT r.source, r.destination, r.id, COUNT(s) as scheduleCount FROM Route r " +
           "LEFT JOIN BusSchedule s ON s.route = r AND s.status <> :cancelledStatus " +
           "WHERE r.status = :activeStatus " +
           "GROUP BY r.source, r.destination, r.id " +
           "ORDER BY scheduleCount DESC")
    List<Object[]> findPopularRoutesByScheduleCount(@Param("cancelledStatus") com.travelease.backend.busbooking.entity.enums.ScheduleStatus cancelledStatus,
                                                    @Param("activeStatus") RouteStatus activeStatus,
                                                    Pageable pageable);

    @Query("SELECT COUNT(s) FROM BusSchedule s WHERE s.route.id = :routeId AND s.status <> :cancelledStatus")
    Long countSchedulesByRouteId(@Param("routeId") Long routeId,
                                 @Param("cancelledStatus") com.travelease.backend.busbooking.entity.enums.ScheduleStatus cancelledStatus);
}
