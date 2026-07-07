package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Price calculator response wrapping the fare breakdown with savings summary.
 * Integration-ready for Booking, Trip Planning, and Analytics modules.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceCalculatorResponse {

    private FareBreakdownResponse breakdown;
    private Double totalPayable;
    private Double totalSavings;
}
