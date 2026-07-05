package com.travelease.backend.busbooking.entity;

import com.travelease.backend.busbooking.entity.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "refund_reference", unique = true, nullable = false)
    private String refundReference;

    @Column(name = "original_amount", nullable = false)
    private Double originalAmount;

    @Column(name = "cancellation_charge", nullable = false)
    @Builder.Default
    private Double cancellationCharge = 0.0;

    @Column(name = "gst_adjustment")
    @Builder.Default
    private Double gstAdjustment = 0.0;

    @Column(name = "coupon_adjustment")
    @Builder.Default
    private Double couponAdjustment = 0.0;

    @Column(name = "net_refundable", nullable = false)
    private Double netRefundable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RefundStatus status = RefundStatus.INITIATED;

    @Column(name = "reason")
    private String reason;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "initiated_at", updatable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
}
