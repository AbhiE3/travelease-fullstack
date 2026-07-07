package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.Conductor;
import com.travelease.backend.busbooking.entity.enums.ConductorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConductorRepository extends JpaRepository<Conductor, Long>, JpaSpecificationExecutor<Conductor> {

    List<Conductor> findByProviderIdAndActiveTrue(Long providerId);

    List<Conductor> findByStatusAndActiveTrue(ConductorStatus status);

    Optional<Conductor> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    @Query("SELECT COUNT(c) FROM Conductor c WHERE c.providerId = :providerId AND c.active = true")
    Long countActiveByProvider(@Param("providerId") Long providerId);

    @Query("SELECT c FROM Conductor c WHERE c.providerId = :providerId AND c.status = 'AVAILABLE' AND c.active = true")
    List<Conductor> findAvailableByProvider(@Param("providerId") Long providerId);
}
