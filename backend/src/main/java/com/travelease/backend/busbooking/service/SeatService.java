package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.SeatLockRequest;
import com.travelease.backend.busbooking.dto.response.SeatLayoutResponse;
import com.travelease.backend.busbooking.dto.response.SeatLockResponse;
import com.travelease.backend.busbooking.dto.response.SeatOccupancyResponse;
import com.travelease.backend.busbooking.entity.enums.SeatStatus;

import java.util.List;

public interface SeatService {

    SeatLayoutResponse getSeats(Long busId, Long scheduleId, SeatStatus status);

    void updateSeatStatus(Long seatId, SeatStatus status);

    SeatLockResponse lockSeats(SeatLockRequest request);

    void unlockSeats(Long scheduleId, List<Long> seatIds);

    SeatOccupancyResponse getOccupancy(Long scheduleId);
}
