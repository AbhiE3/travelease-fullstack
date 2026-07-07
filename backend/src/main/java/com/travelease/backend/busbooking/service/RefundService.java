package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.RefundStatusTransitionRequest;
import com.travelease.backend.busbooking.dto.response.RefundResponse;
import com.travelease.backend.busbooking.entity.enums.RefundStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RefundService {

    RefundResponse initiateRefund(Long bookingId, Double amount, String reason);
    RefundResponse processRefund(Long refundId);
    RefundResponse approveRefund(Long refundId);
    RefundResponse completeRefund(Long refundId);
    RefundResponse rejectRefund(Long refundId, String rejectionReason);
    RefundResponse failRefund(Long refundId, String failureReason);

    RefundResponse transitionRefund(Long id, RefundStatusTransitionRequest request);

    RefundResponse getRefundById(Long refundId);
    List<RefundResponse> getRefunds(String reference, Long bookingId, RefundStatus status, Pageable pageable);
}
