package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search suggestion based on user's previous bookings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchSuggestionResponse {

    private Long routeId;
    private String source;
    private String destination;
    private Long bookingCount;
}
