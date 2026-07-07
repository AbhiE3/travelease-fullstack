package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long>, JpaSpecificationExecutor<Discount> {

    @Query("SELECT d FROM Discount d WHERE d.active = true " +
           "AND (d.validFrom IS NULL OR d.validFrom <= :today) " +
           "AND (d.validTo IS NULL OR d.validTo >= :today)")
    List<Discount> findActiveDiscounts(@Param("today") LocalDate today);

    long countByActiveTrue();
}
