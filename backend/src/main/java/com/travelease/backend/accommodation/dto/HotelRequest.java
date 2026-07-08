package com.travelease.backend.accommodation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record HotelRequest(
        @NotNull Integer destinationId,
        @NotBlank String hotelName,
        @NotBlank String address,
        @DecimalMin("0.0") BigDecimal rating,
        @Positive BigDecimal pricePerNight,
        String amenities,
        @NotBlank String status,
        /**
         * Only meaningful for ROLE_ADMIN callers of createHotel, as the explicit
         * target Hotel Provider tenant. Ignored/not-authoritative for
         * ROLE_HOTEL_PROVIDER callers (their own providerId always wins - see
         * SecurityUtil.resolveEffectiveHotelProviderId) and unused entirely on
         * updateHotel, since ownership is fixed at creation and is not
         * reassignable via this request.
         */
        Long providerId
) {
}
