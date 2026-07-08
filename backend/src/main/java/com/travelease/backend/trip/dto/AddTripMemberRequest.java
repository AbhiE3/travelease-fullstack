package com.travelease.backend.trip.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddTripMemberRequest(
        @NotBlank @Email String email
) {
}
