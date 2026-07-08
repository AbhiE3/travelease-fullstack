package com.travelease.backend.itinerary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ActivitySlotResponse(
        UUID activitySlotId,
        String activityId,
        LocalDate activityDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price,
        Integer capacity,
        /**
         * capacity minus the sum of participants across CONFIRMED/ATTENDED/
         * NO_SHOW bookings for this slot - computed on read, never persisted
         * (see ActivityBooking capacity-accounting design). A momentarily
         * stale read under concurrent load is acceptable; the authoritative
         * check happens under a pessimistic lock at booking-creation time.
         */
        Integer remainingCapacity
) {
}
