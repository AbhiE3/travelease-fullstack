package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.response.SeatOccupancyResponse;

public interface SeatOccupancyService {

    SeatOccupancyResponse getOccupancy(Long scheduleId);
}
