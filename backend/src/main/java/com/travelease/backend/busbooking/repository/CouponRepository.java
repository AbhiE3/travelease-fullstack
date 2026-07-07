package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT c FROM Coupon c WHERE c.active = true " +
           "AND c.validFrom <= :today AND c.validTo >= :today " +
           "AND (c.maxUsage IS NULL OR c.usedCount < c.maxUsage)")
    List<Coupon> findValidCoupons(@Param("today") LocalDate today);

    long countByActiveTrue();
}
