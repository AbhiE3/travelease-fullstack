package com.travelease.backend.accommodation.dto;

import jakarta.validation.constraints.NotBlank;

public record HotelPolicyRequest(@NotBlank String policies) {
}
