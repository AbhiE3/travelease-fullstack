package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.SeatLockRequest;
import com.travelease.backend.busbooking.dto.response.SeatLayoutResponse;
import com.travelease.backend.busbooking.dto.response.SeatLockResponse;
import com.travelease.backend.busbooking.dto.response.SeatOccupancyResponse;
import com.travelease.backend.busbooking.dto.response.SeatResponse;
import com.travelease.backend.busbooking.entity.BusSchedule;
import com.travelease.backend.busbooking.entity.Bus;
import com.travelease.backend.busbooking.entity.Seat;
import com.travelease.backend.busbooking.entity.enums.SeatStatus;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.mapper.SeatMapper;
import com.travelease.backend.busbooking.repository.BusRepository;
import com.travelease.backend.busbooking.repository.BusScheduleRepository;
import com.travelease.backend.busbooking.repository.SeatRepository;
import com.travelease.backend.busbooking.service.SeatAllocationService;
import com.travelease.backend.busbooking.service.SeatOccupancyService;
import com.travelease.backend.busbooking.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final BusRepository busRepository;
    private final BusScheduleRepository scheduleRepository;
    private final SeatMapper seatMapper;
    private final SeatAllocationService seatAllocationService;
    private final SeatOccupancyService seatOccupancyService;

    @Override
    @Transactional(readOnly = true)
    public SeatLayoutResponse getSeats(Long busId, Long scheduleId, SeatStatus status) {
        if (busId == null && scheduleId == null) {
            throw new IllegalArgumentException("Either busId or scheduleId must be provided");
        }

        Bus bus;
        List<Seat> seats;

        if (busId != null) {
            bus = busRepository.findById(busId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", busId));
            seats = status != null ? seatRepository.findByBusIdAndStatus(busId, status) : seatRepository.findByBusId(busId);
        } else {
            BusSchedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));
            bus = schedule.getBus();
            seats = status != null && status != SeatStatus.AVAILABLE
                    ? seatRepository.findByBusIdAndStatus(bus.getId(), status)
                    : seatRepository.findAvailableSeatsForSchedule(scheduleId);
        }

        List<SeatResponse> seatResponses = seats.stream()
                .map(seatMapper::toResponse)
                .toList();

        return SeatLayoutResponse.builder()
                .busId(bus.getId())
                .busName(bus.getBusName())
                .seats(seatResponses)
                .build();
    }

    @Override
    @Transactional
    public void updateSeatStatus(Long seatId, SeatStatus status) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", "id", seatId));
        seat.setStatus(status);
        seatRepository.save(seat);
    }

    @Override
    @Transactional
    public SeatLockResponse lockSeats(SeatLockRequest request) {
        return seatAllocationService.lockSeats(request);
    }

    @Override
    @Transactional
    public void unlockSeats(Long scheduleId, List<Long> seatIds) {
        seatAllocationService.unlockSeats(scheduleId, seatIds);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatOccupancyResponse getOccupancy(Long scheduleId) {
        return seatOccupancyService.getOccupancy(scheduleId);
    }
}
