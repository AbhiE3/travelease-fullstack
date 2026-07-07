package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KPI card structure for dashboard display.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiCard {

    private String title;
    private Double value;
    private String unit;
    private Double changePercent;
    private String trend; // UP, DOWN, STABLE
    private String icon;
}
