package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.CancellationPolicy;
import com.travelease.backend.busbooking.entity.enums.BusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {

    @Query("SELECT c FROM CancellationPolicy c WHERE c.active = true " +
           "AND (c.routeId = :routeId OR c.routeId IS NULL) " +
           "AND (c.busType = :busType OR c.busType IS NULL) " +
           "ORDER BY c.routeId DESC NULLS LAST, c.busType DESC NULLS LAST")
    List<CancellationPolicy> findApplicablePolicies(
            @Param("routeId") Long routeId,
            @Param("busType") BusType busType);

    List<CancellationPolicy> findByActiveTrue();
}
