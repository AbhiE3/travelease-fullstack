package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.response.SeatOccupancyResponse;
import com.travelease.backend.busbooking.entity.BusSchedule;
import com.travelease.backend.busbooking.entity.enums.SeatLockStatus;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.repository.BusScheduleRepository;
import com.travelease.backend.busbooking.repository.SeatLockRepository;
import com.travelease.backend.busbooking.repository.SeatRepository;
import com.travelease.backend.busbooking.service.SeatOccupancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatOccupancyServiceImpl implements SeatOccupancyService {

    private final SeatRepository seatRepository;
    private final SeatLockRepository seatLockRepository;
    private final BusScheduleRepository scheduleRepository;

    @Override
    @Transactional(readOnly = true)
    public SeatOccupancyResponse getOccupancy(Long scheduleId) {
        BusSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        int totalSeats = schedule.getBus().getTotalSeats();

        int availableFromQuery = seatRepository.findAvailableSeatsForSchedule(scheduleId).size();

        List<Long> lockedSeatIds = seatLockRepository.findLockedSeatIdsByScheduleId(
                scheduleId, SeatLockStatus.LOCKED, LocalDateTime.now());
        int lockedSeats = lockedSeatIds.size();

        int availableSeats = availableFromQuery - lockedSeats;
        if (availableSeats < 0) {
            availableSeats = 0;
        }

        int bookedSeats = totalSeats - availableFromQuery;
        if (bookedSeats < 0) {
            bookedSeats = 0;
        }

        double occupancyPercentage = totalSeats > 0
                ? ((bookedSeats + lockedSeats) * 100.0) / totalSeats
                : 0.0;

        return SeatOccupancyResponse.builder()
                .scheduleId(scheduleId)
                .totalSeats(totalSeats)
                .bookedSeats(bookedSeats)
                .availableSeats(availableSeats)
                .lockedSeats(lockedSeats)
                .occupancyPercentage(Math.round(occupancyPercentage * 100.0) / 100.0)
                .build();
    }
}
