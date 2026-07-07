package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.CancellationReason;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancellationRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Cancellation reason is required")
    private CancellationReason reason;

    private String reasonText; // optional additional details
}
