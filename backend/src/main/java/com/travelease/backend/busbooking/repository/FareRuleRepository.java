package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.FareRule;
import com.travelease.backend.busbooking.entity.enums.BusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FareRuleRepository extends JpaRepository<FareRule, Long>, JpaSpecificationExecutor<FareRule> {

    Optional<FareRule> findByRouteIdAndBusType(Long routeId, BusType busType);

    Optional<FareRule> findByRouteIdAndBusTypeIsNull(Long routeId);

    List<FareRule> findByRouteIdAndActiveTrue(Long routeId);

    @Query("SELECT f FROM FareRule f WHERE f.routeId = :routeId AND f.active = true " +
           "AND (f.busType = :busType OR f.busType IS NULL) " +
           "ORDER BY f.busType DESC NULLS LAST")
    List<FareRule> findApplicableFareRules(@Param("routeId") Long routeId, @Param("busType") BusType busType);

    long countByActiveTrue();
}
