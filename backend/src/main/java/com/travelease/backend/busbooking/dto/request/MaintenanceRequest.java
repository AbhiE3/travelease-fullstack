package com.travelease.backend.busbooking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequest {

    @NotNull(message = "Bus ID is required")
    private Long busId;

    @NotNull(message = "Maintenance type is required")
    private String maintenanceType;

    private String description;

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    private Double estimatedCost;

    private LocalDate nextMaintenanceDate;

    private String performedBy;
}
