package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import com.travelease.backend.busbooking.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    // Core
    private Long id;
    private String bookingReference;
    private BookingStatus status;
    private Double totalFare;

    // Ticket
    private String ticketNumber;
    private String qrCodeString;

    // Payment
    private PaymentStatus paymentStatus;

    // Contact
    private String contactEmail;
    private String contactPhone;

    // Coupon
    private String couponCode;
    private Double couponDiscount;

    // Timestamps
    private LocalDateTime bookedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;

    // Integration-ready fields
    private Long scheduleId;
    private Long routeId;
    private Long providerId;
    private Long busId;
    private Long userId;

    // Nested
    private ScheduleResponse schedule;
    private List<BookingSeatResponse> seats;
    private List<BookingTimelineResponse> timeline;
}
