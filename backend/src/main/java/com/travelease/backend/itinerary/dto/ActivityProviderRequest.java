package com.travelease.backend.itinerary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ActivityProviderRequest(
        @NotNull Integer destinationId,
        @NotBlank String activityName,
        @Positive Double durationHours,
        @NotBlank String startTime,
        @NotBlank String endTime,
        String description,
        /**
         * Only meaningful for ROLE_ADMIN callers of create, as the explicit
         * target Activity Provider tenant. Ignored/not-authoritative for
         * ROLE_ACTIVITY_PROVIDER callers (their own providerId always wins -
         * see SecurityUtil.resolveEffectiveActivityProviderId) and unused on
         * update, since ownership is fixed at creation and not reassignable
         * via this request.
         */
        Long providerId
) {
}
