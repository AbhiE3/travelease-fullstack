package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a popular route based on search or booking frequency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopularRouteResponse {

    private Long routeId;
    private String source;
    private String destination;
    private Long searchCount;
    private Long totalSchedules;
}
