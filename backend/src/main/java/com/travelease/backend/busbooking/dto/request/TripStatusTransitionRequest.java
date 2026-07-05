package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.TripStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripStatusTransitionRequest {

    @NotNull(message = "Target status is required")
    private TripStatus status;

    private Integer delayMinutes;    // For DELAYED status
    private Double distanceCoveredKm; // For COMPLETED status
    private String reason;           // For CANCELLED status
}
