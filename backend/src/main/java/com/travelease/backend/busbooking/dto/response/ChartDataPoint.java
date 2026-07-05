package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic data point for charts (line, bar, pie).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartDataPoint {

    private String label;
    private Double value;
    private String category;
    private String color;
}
