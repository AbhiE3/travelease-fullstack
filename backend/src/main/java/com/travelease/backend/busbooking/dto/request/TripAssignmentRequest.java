package com.travelease.backend.busbooking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripAssignmentRequest {

    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    private Long driverId;
    private Long conductorId;

    private String notes;
}
