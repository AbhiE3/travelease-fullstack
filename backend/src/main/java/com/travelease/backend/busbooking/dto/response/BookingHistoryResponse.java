package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingHistoryResponse {

    private Long id;
    private String bookingReference;
    private String source;
    private String destination;
    private LocalDate travelDate;
    private LocalTime departureTime;
    private Double totalFare;
    private BookingStatus status;
    private String busName;
    private Integer seatsBooked;
    private LocalDateTime bookedAt;
    private String ticketNumber;
    private String paymentStatus;
}
