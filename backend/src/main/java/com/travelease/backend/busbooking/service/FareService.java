package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.FareCalculationRequest;
import com.travelease.backend.busbooking.dto.request.FareRequest;
import com.travelease.backend.busbooking.dto.response.CancellationPreviewResponse;
import com.travelease.backend.busbooking.dto.response.FareBreakdownResponse;
import com.travelease.backend.busbooking.dto.response.FareResponse;
import com.travelease.backend.busbooking.dto.response.PriceCalculatorResponse;
import com.travelease.backend.busbooking.entity.enums.BusType;

import java.util.List;

public interface FareService {

    FareResponse createFareRule(FareRequest request);

    FareResponse updateFareRule(Long id, FareRequest request);

    void deleteFareRule(Long id);

    FareResponse getFareRuleById(Long id);

    List<FareResponse> getFareRules(Long routeId, BusType busType, Boolean active);

    FareBreakdownResponse calculateFareBreakdown(FareCalculationRequest request);

    PriceCalculatorResponse calculatePrice(FareCalculationRequest request);

    CancellationPreviewResponse getCancellationPreview(Long scheduleId, Double totalFare);
}
