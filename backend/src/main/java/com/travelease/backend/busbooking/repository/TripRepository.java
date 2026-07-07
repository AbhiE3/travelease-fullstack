package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.Trip;
import com.travelease.backend.busbooking.entity.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("busbookingTripRepository")
public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {

    List<Trip> findByScheduleId(Long scheduleId);

    Optional<Trip> findByScheduleIdAndStatus(Long scheduleId, TripStatus status);

    List<Trip> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    List<Trip> findByConductorIdOrderByCreatedAtDesc(Long conductorId);

    List<Trip> findByStatus(TripStatus status);

    @Query("SELECT COUNT(t) FROM BusTrip t WHERE t.driver.id = :driverId AND t.status = 'COMPLETED'")
    Long countCompletedTripsByDriver(@Param("driverId") Long driverId);

    @Query("SELECT COUNT(t) FROM BusTrip t WHERE t.conductor.id = :conductorId AND t.status = 'COMPLETED'")
    Long countCompletedTripsByConductor(@Param("conductorId") Long conductorId);

    @Query("SELECT COALESCE(SUM(t.distanceCoveredKm), 0.0) FROM BusTrip t WHERE t.driver.id = :driverId AND t.status = 'COMPLETED'")
    Double sumDistanceByDriver(@Param("driverId") Long driverId);

    // Fix C-3: Provider-scoped trip query (avoids loading all trips)
    @Query("SELECT t FROM BusTrip t WHERE t.schedule.bus.providerId = :providerId")
    List<Trip> findByProviderId(@Param("providerId") Long providerId);
}
