package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Integration-ready cancellation response.
 * Exposes all information needed by future modules (Analytics, Notification, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationResponse {

    private Long bookingId;
    private String bookingReference;
    private BookingStatus status;

    // Cancellation details
    private String reason;
    private String reasonText;
    private Boolean partialCancellation;
    private List<Long> cancelledSeatIds;
    private Integer totalCancelledSeats;

    // Financial breakdown
    private Double originalFare;
    private Double cancellationCharge;
    private Double refundAmount;
    private Double netPayableAfterCancellation;

    // Refund
    private RefundResponse refund;

    // Ticket status
    private String ticketStatus;
}
