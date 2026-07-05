package com.travelease.backend.itinerary.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryRequest {

    private String tripId;
    private String activityId;
    private LocalDate activityDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}