package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceStatusTransitionRequest {

    @NotNull(message = "Target status is required")
    private MaintenanceStatus status;

    private Double cost;           // For COMPLETED status
    private LocalDate completedDate; // For COMPLETED status
}
