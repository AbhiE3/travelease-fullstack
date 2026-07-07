package com.travelease.backend.busbooking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> seatIds;

    @NotEmpty(message = "Passenger details are required")
    @Valid
    private List<PassengerDetailDto> passengerDetails;

    private String couponCode;
    private String contactEmail;
    private String contactPhone;
}
