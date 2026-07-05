package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Integration-ready refund response.
 * Exposes all information needed by future modules (Analytics, Notification, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {

    private Long id;
    private String refundReference;
    private Long bookingId;
    private String bookingReference;

    // Financial breakdown
    private Double originalAmount;
    private Double cancellationCharge;
    private Double gstAdjustment;
    private Double couponAdjustment;
    private Double netRefundable;

    // Status
    private RefundStatus status;
    private String reason;
    private String rejectionReason;

    // Timestamps
    private LocalDateTime initiatedAt;
    private LocalDateTime processedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private LocalDateTime rejectedAt;
}
