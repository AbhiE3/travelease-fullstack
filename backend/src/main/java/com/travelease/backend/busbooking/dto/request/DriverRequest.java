package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverRequest {

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    @NotBlank(message = "Driver name is required")
    private String name;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    private String phone;
    private String email;

    // Optional: when present and different from the driver's current status,
    // updateDriver applies the transition. Lets PUT /drivers/{id} subsume the
    // status-PATCH endpoint that used to live at PATCH /drivers/{id}/status.
    private DriverStatus status;
}
