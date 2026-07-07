package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.SeatLockRequest;
import com.travelease.backend.busbooking.dto.response.SeatLockResponse;
import com.travelease.backend.busbooking.entity.BusSchedule;
import com.travelease.backend.busbooking.entity.Seat;
import com.travelease.backend.busbooking.entity.SeatLock;
import com.travelease.backend.busbooking.entity.enums.ScheduleStatus;
import com.travelease.backend.busbooking.entity.enums.SeatLockStatus;
import com.travelease.backend.busbooking.entity.enums.SeatType;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.exception.SeatUnavailableException;
import com.travelease.backend.busbooking.repository.BusScheduleRepository;
import com.travelease.backend.busbooking.repository.SeatLockRepository;
import com.travelease.backend.busbooking.repository.SeatRepository;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.busbooking.service.SeatAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatAllocationServiceImpl implements SeatAllocationService {

    private static final int LOCK_TIMEOUT_MINUTES = 5;

    private final SeatLockRepository seatLockRepository;
    private final SeatRepository seatRepository;
    private final BusScheduleRepository scheduleRepository;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public SeatLockResponse lockSeats(SeatLockRequest request) {
        java.util.UUID userId = securityUtil.getCurrentUserId();
        Long scheduleId = request.getScheduleId();

        BusSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        if (schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new SeatUnavailableException("Cannot lock seats for a cancelled schedule");
        }

        List<Long> availableSeatIds = seatRepository.findAvailableSeatsForSchedule(scheduleId)
                .stream()
                .map(Seat::getId)
                .toList();

        List<Long> activeLockedSeatIds = seatLockRepository.findLockedSeatIdsByScheduleId(
                scheduleId, SeatLockStatus.LOCKED, LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(LOCK_TIMEOUT_MINUTES);
        List<Long> lockedSeatIds = new ArrayList<>();

        for (Long seatId : request.getSeatIds()) {
            if (!availableSeatIds.contains(seatId)) {
                throw new SeatUnavailableException(
                        "Seat with ID " + seatId + " is not available for this schedule");
            }

            if (activeLockedSeatIds.contains(seatId)) {
                Optional<SeatLock> existingLock = seatLockRepository
                        .findBySeatIdAndScheduleIdAndStatus(seatId, scheduleId, SeatLockStatus.LOCKED);

                if (existingLock.isPresent() && existingLock.get().getExpiresAt().isAfter(now)) {
                    if (!existingLock.get().getUserId().equals(userId)) {
                        throw new SeatUnavailableException(
                                "Seat with ID " + seatId + " is currently locked by another user");
                    }
                    existingLock.get().setExpiresAt(expiresAt);
                    seatLockRepository.save(existingLock.get());
                    lockedSeatIds.add(seatId);
                    continue;
                }
            }

            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat", "id", seatId));

            SeatLock lock = SeatLock.builder()
                    .seat(seat)
                    .scheduleId(scheduleId)
                    .userId(userId)
                    .expiresAt(expiresAt)
                    .status(SeatLockStatus.LOCKED)
                    .build();
            seatLockRepository.save(lock);
            lockedSeatIds.add(seatId);
        }

        return SeatLockResponse.builder()
                .scheduleId(scheduleId)
                .lockedSeatIds(lockedSeatIds)
                .lockedAt(now)
                .expiresAt(expiresAt)
                .message("Seats locked successfully. Locks expire at " + expiresAt)
                .build();
    }

    @Override
    @Transactional
    public void unlockSeats(Long scheduleId, List<Long> seatIds) {
        java.util.UUID userId = securityUtil.getCurrentUserId();

        for (Long seatId : seatIds) {
            Optional<SeatLock> lock = seatLockRepository
                    .findBySeatIdAndScheduleIdAndStatus(seatId, scheduleId, SeatLockStatus.LOCKED);

            if (lock.isPresent() && lock.get().getExpiresAt().isAfter(LocalDateTime.now())) {
                if (!lock.get().getUserId().equals(userId)) {
                    throw new SeatUnavailableException(
                            "Cannot unlock seat " + seatId + " â€” locked by another user");
                }
                lock.get().setStatus(SeatLockStatus.RELEASED);
                seatLockRepository.save(lock.get());
            }
        }
    }

    @Override
    @Transactional
    public void releaseExpiredLocks() {
        int expired = seatLockRepository.expireLocks(
                SeatLockStatus.LOCKED, SeatLockStatus.EXPIRED, LocalDateTime.now());
        if (expired > 0) {
            log.info("Released {} expired seat locks", expired);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateSeatsForBooking(Long scheduleId, List<Long> seatIds, java.util.UUID userId) {
        List<Long> availableSeatIds = seatRepository.findAvailableSeatsForSchedule(scheduleId)
                .stream()
                .map(Seat::getId)
                .toList();

        List<Long> activeLockedSeatIds = seatLockRepository.findLockedSeatIdsByScheduleId(
                scheduleId, SeatLockStatus.LOCKED, LocalDateTime.now());

        for (Long seatId : seatIds) {
            if (!availableSeatIds.contains(seatId)) {
                throw new SeatUnavailableException(
                        "Seat with ID " + seatId + " is not available for this schedule");
            }

            if (activeLockedSeatIds.contains(seatId)) {
                Optional<SeatLock> lock = seatLockRepository
                        .findBySeatIdAndScheduleIdAndStatus(seatId, scheduleId, SeatLockStatus.LOCKED);

                if (lock.isPresent()
                        && lock.get().getExpiresAt().isAfter(LocalDateTime.now())
                        && !lock.get().getUserId().equals(userId)) {
                    throw new SeatUnavailableException(
                            "Seat with ID " + seatId + " is locked by another user");
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> findConsecutiveSeats(Long scheduleId, int count, SeatType preference) {
        List<Seat> availableSeats = seatRepository.findAvailableSeatsForSchedule(scheduleId);

        List<Long> lockedSeatIds = seatLockRepository.findLockedSeatIdsByScheduleId(
                scheduleId, SeatLockStatus.LOCKED, LocalDateTime.now());

        List<Seat> candidateSeats = availableSeats.stream()
                .filter(seat -> !lockedSeatIds.contains(seat.getId()))
                .collect(Collectors.toList());

        if (preference != null) {
            List<Seat> preferred = candidateSeats.stream()
                    .filter(seat -> seat.getSeatType() == preference)
                    .collect(Collectors.toList());
            List<Seat> result = findConsecutiveInGroup(preferred, count);
            if (result != null) {
                return result;
            }
        }

        Map<Integer, List<Seat>> byDeck = candidateSeats.stream()
                .collect(Collectors.groupingBy(Seat::getDeck));

        for (List<Seat> deckSeats : byDeck.values()) {
            List<Seat> result = findConsecutiveInGroup(deckSeats, count);
            if (result != null) {
                return result;
            }
        }

        return Collections.emptyList();
    }

    @Override
    public void validateLadiesSeats(List<Seat> seats, Map<Long, String> passengerGenderBySeatId) {
        for (Seat seat : seats) {
            if (seat.getSeatType() == SeatType.LADIES) {
                String gender = passengerGenderBySeatId.get(seat.getId());
                if (gender == null || !gender.equalsIgnoreCase("FEMALE")) {
                    throw new SeatUnavailableException(
                            "Seat " + seat.getSeatNumber()
                                    + " is reserved for ladies. Passenger gender must be FEMALE.");
                }
            }
        }
    }

    @Override
    @Transactional
    public void releaseLocksForBooking(Long scheduleId, List<Long> seatIds, java.util.UUID userId) {
        for (Long seatId : seatIds) {
            Optional<SeatLock> lock = seatLockRepository
                    .findBySeatIdAndScheduleIdAndStatus(seatId, scheduleId, SeatLockStatus.LOCKED);

            if (lock.isPresent() && lock.get().getUserId().equals(userId)) {
                lock.get().setStatus(SeatLockStatus.RELEASED);
                seatLockRepository.save(lock.get());
            }
        }
    }

    private List<Seat> findConsecutiveInGroup(List<Seat> seats, int count) {
        if (seats.size() < count) {
            return null;
        }

        seats.sort(Comparator.comparingInt(this::extractSeatNumber));

        List<Seat> currentRun = new ArrayList<>();
        currentRun.add(seats.get(0));

        for (int i = 1; i < seats.size(); i++) {
            int prevNum = extractSeatNumber(seats.get(i - 1));
            int currNum = extractSeatNumber(seats.get(i));

            if (currNum == prevNum + 1) {
                currentRun.add(seats.get(i));
                if (currentRun.size() == count) {
                    return currentRun;
                }
            } else {
                currentRun.clear();
                currentRun.add(seats.get(i));
            }
        }

        return null;
    }

    private int extractSeatNumber(Seat seat) {
        String numberPart = seat.getSeatNumber().replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
