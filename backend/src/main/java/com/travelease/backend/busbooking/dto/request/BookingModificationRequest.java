package com.travelease.backend.busbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingModificationRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    private List<PassengerDetailDto> updatedPassengerDetails;

    private String contactEmail;
    private String contactPhone;
}
