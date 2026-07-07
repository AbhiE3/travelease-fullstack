package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.Maintenance;
import com.travelease.backend.busbooking.entity.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long>, JpaSpecificationExecutor<Maintenance> {

    List<Maintenance> findByBusIdOrderByScheduledDateDesc(Long busId);

    List<Maintenance> findByBusIdAndStatus(Long busId, MaintenanceStatus status);

    @Query("SELECT m FROM Maintenance m WHERE m.bus.id = :busId AND m.status IN :statuses")
    List<Maintenance> findByBusIdAndStatusIn(@Param("busId") Long busId, @Param("statuses") List<MaintenanceStatus> statuses);

    @Query("SELECT COUNT(m) FROM Maintenance m WHERE m.bus.id = :busId AND m.status IN ('SCHEDULED', 'IN_PROGRESS')")
    Long countActiveMaintenanceByBus(@Param("busId") Long busId);

    @Query("SELECT COALESCE(SUM(m.cost), 0.0) FROM Maintenance m WHERE m.bus.id = :busId AND m.status = 'COMPLETED'")
    Double sumCompletedCostByBus(@Param("busId") Long busId);

    List<Maintenance> findByStatusAndScheduledDateBefore(MaintenanceStatus status, LocalDate date);

    // Fix: maintenance analytics previously loaded the entire maintenance table (findAll())
    // and filtered to the provider's buses in memory. This filters at the database level instead.
    @Query("SELECT m FROM Maintenance m WHERE m.bus.providerId = :providerId")
    List<Maintenance> findByProviderId(@Param("providerId") Long providerId);
}
