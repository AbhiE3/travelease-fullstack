package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.RefundStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundStatusTransitionRequest {

    @NotNull(message = "Target status is required")
    private RefundStatus status;

    private String reason; // Required for REJECTED and FAILED transitions
}
