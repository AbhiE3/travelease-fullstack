package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Integration-ready ticket response.
 * Exposes all information needed by future modules (Itinerary, Analytics, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    private Long bookingId;
    private String bookingReference;
    private String ticketNumber;
    private String qrCodeString;
    private BookingStatus status;

    // Schedule context
    private Long scheduleId;
    private Long routeId;
    private Long providerId;
    private Long busId;
    private String busName;
    private String busNumber;
    private String source;
    private String destination;
    private LocalDate travelDate;

    // Passenger
    private String primaryPassengerName;
    private Integer totalPassengers;

    // Fare
    private Double totalFare;

    // Timestamps
    private LocalDateTime bookedAt;
    private LocalDateTime confirmedAt;
}
