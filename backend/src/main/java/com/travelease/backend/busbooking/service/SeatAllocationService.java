package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.SeatLockRequest;
import com.travelease.backend.busbooking.dto.response.SeatLockResponse;
import com.travelease.backend.busbooking.entity.Seat;
import com.travelease.backend.busbooking.entity.enums.SeatType;

import java.util.List;
import java.util.Map;

public interface SeatAllocationService {

    SeatLockResponse lockSeats(SeatLockRequest request);

    void unlockSeats(Long scheduleId, List<Long> seatIds);

    void releaseExpiredLocks();

    void validateSeatsForBooking(Long scheduleId, List<Long> seatIds, Long userId);

    List<Seat> findConsecutiveSeats(Long scheduleId, int count, SeatType preference);

    void validateLadiesSeats(List<Seat> seats, Map<Long, String> passengerGenderBySeatId);

    void releaseLocksForBooking(Long scheduleId, List<Long> seatIds, Long userId);
}
