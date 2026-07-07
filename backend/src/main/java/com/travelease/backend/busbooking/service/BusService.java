package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.BusRequest;
import com.travelease.backend.busbooking.dto.response.BusResponse;
import com.travelease.backend.busbooking.entity.enums.BusStatus;

import java.util.List;

public interface BusService {

    BusResponse createBus(BusRequest request);

    BusResponse updateBus(Long id, BusRequest request);

    void deleteBus(Long id);

    BusResponse getBusById(Long id);

    List<BusResponse> getBuses(Long providerId, BusStatus status);
}
