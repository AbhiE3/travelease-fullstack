package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.Refund;
import com.travelease.backend.busbooking.entity.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long>, JpaSpecificationExecutor<Refund> {

    List<Refund> findByBookingIdOrderByInitiatedAtDesc(Long bookingId);

    Optional<Refund> findByRefundReference(String refundReference);

    List<Refund> findByBookingUserIdOrderByInitiatedAtDesc(UUID userId);

    @Query("SELECT COUNT(r) FROM Refund r WHERE r.status = :status")
    Long countByStatus(@Param("status") RefundStatus status);

    @Query("SELECT COALESCE(SUM(r.netRefundable), 0.0) FROM Refund r WHERE r.status = 'COMPLETED'")
    Double sumCompletedRefunds();
}
