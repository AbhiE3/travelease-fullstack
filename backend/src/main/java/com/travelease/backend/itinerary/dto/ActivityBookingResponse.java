package com.travelease.backend.itinerary.dto;

import com.travelease.backend.itinerary.entity.ActivityBookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ActivityBookingResponse(
        UUID bookingId,
        UUID activitySlotId,
        String activityId,
        String activityName,
        LocalDate activityDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer participantCount,
        BigDecimal pricePerParticipant,
        BigDecimal totalAmount,
        ActivityBookingStatus status,
        LocalDateTime bookedAt,
        UUID bookedByUserId
) {
}
