package com.travelease.backend.busbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatLockScheduler {

    private final SeatAllocationService seatAllocationService;

    @Scheduled(fixedRate = 60000)
    public void releaseExpiredLocks() {
        seatAllocationService.releaseExpiredLocks();
    }
}
