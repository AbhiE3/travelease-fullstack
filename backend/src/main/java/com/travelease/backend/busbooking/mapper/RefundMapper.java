package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.response.RefundResponse;
import com.travelease.backend.busbooking.entity.Refund;
import org.springframework.stereotype.Component;

@Component
public class RefundMapper {

    public RefundResponse toResponse(Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .refundReference(refund.getRefundReference())
                .bookingId(refund.getBooking().getId())
                .bookingReference(refund.getBooking().getBookingReference())
                .originalAmount(refund.getOriginalAmount())
                .cancellationCharge(refund.getCancellationCharge())
                .gstAdjustment(refund.getGstAdjustment())
                .couponAdjustment(refund.getCouponAdjustment())
                .netRefundable(refund.getNetRefundable())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .rejectionReason(refund.getRejectionReason())
                .initiatedAt(refund.getInitiatedAt())
                .processedAt(refund.getProcessedAt())
                .approvedAt(refund.getApprovedAt())
                .completedAt(refund.getCompletedAt())
                .failedAt(refund.getFailedAt())
                .rejectedAt(refund.getRejectedAt())
                .build();
    }
}
