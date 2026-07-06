package com.travelease.backend.accommodation.dto;

import java.util.List;

public record BookingValidationResponse(boolean valid, List<String> errors) {
}
