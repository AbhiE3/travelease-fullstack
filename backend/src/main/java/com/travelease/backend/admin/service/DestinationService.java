package com.travelease.backend.admin.service;

import com.travelease.backend.admin.dto.DestinationResponse;

import java.util.List;

public interface DestinationService {
    List<DestinationResponse> getAllDestinations();
    DestinationResponse getDestinationById(Integer destinationId);
}
