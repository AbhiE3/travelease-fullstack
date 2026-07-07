package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.RefundStatusTransitionRequest;
import com.travelease.backend.busbooking.dto.response.ApiResponse;
import com.travelease.backend.busbooking.dto.response.RefundResponse;
import com.travelease.backend.busbooking.entity.enums.RefundStatus;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.busbooking.service.BookingService;
import com.travelease.backend.busbooking.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Refunds are owned indirectly via their Booking (Refund.bookingId -> Booking.userId),
 * not by provider. Ownership reuses BookingService.getBookingById(), which throws
 * AccessDeniedException for a non-owning, non-admin caller (see
 * BookingServiceImpl.ensureOwnership).
 */
@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
@Tag(name = "Refund Management", description = "Endpoints for managing refunds")
public class RefundController {

    private final RefundService refundService;
    private final BookingService bookingService;
    private final SecurityUtil securityUtil;

    @GetMapping
    @Operation(summary = "Get refunds with optional filters")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getRefunds(
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) Long bookingId,
            @RequestParam(required = false) RefundStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        if (bookingId != null) {
            bookingService.getBookingById(bookingId); // ownership check side effect
        } else if (!securityUtil.getCurrentUserRoles().contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Only ADMIN may list refunds without a bookingId filter");
        }
        List<RefundResponse> response = refundService.getRefunds(reference, bookingId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Refunds fetched successfully", response, "/api/refunds"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get refund by ID")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefundById(@PathVariable Long id) {
        RefundResponse response = refundService.getRefundById(id);
        bookingService.getBookingById(response.getBookingId()); // ownership check side effect
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Refund fetched successfully", response, "/api/refunds/" + id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Transition refund status (process/approve/complete/reject/fail)")
    public ResponseEntity<ApiResponse<RefundResponse>> transitionRefundStatus(
            @PathVariable Long id,
            @Valid @RequestBody RefundStatusTransitionRequest request) {
        RefundResponse response = refundService.transitionRefund(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Refund status updated successfully", response, "/api/refunds/" + id + "/status"));
    }
}
