package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.Driver;
import com.travelease.backend.busbooking.entity.enums.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long>, JpaSpecificationExecutor<Driver> {

    List<Driver> findByProviderIdAndActiveTrue(Long providerId);

    List<Driver> findByStatusAndActiveTrue(DriverStatus status);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("SELECT COUNT(d) FROM Driver d WHERE d.providerId = :providerId AND d.active = true")
    Long countActiveByProvider(@Param("providerId") Long providerId);

    @Query("SELECT d FROM Driver d WHERE d.providerId = :providerId AND d.status = 'AVAILABLE' AND d.active = true")
    List<Driver> findAvailableByProvider(@Param("providerId") Long providerId);
}
