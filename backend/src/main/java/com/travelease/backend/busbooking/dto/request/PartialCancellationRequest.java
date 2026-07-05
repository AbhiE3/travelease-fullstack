package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.CancellationReason;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartialCancellationRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotEmpty(message = "At least one seat ID is required")
    private List<Long> seatIds;

    @NotNull(message = "Cancellation reason is required")
    private CancellationReason reason;

    private String reasonText;
}
