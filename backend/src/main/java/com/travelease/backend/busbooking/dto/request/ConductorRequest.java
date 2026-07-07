package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.ConductorStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConductorRequest {

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    @NotBlank(message = "Conductor name is required")
    private String name;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    private String phone;
    private String email;

    // Optional: when present and different from the conductor's current status,
    // updateConductor applies the transition. Lets PUT /conductors/{id} subsume
    // the status-PATCH endpoint that used to live at PATCH /conductors/{id}/status.
    private ConductorStatus status;
}
