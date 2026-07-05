package com.travelease.backend.accommodation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReviewRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal rating,
        String comment
) {
}
