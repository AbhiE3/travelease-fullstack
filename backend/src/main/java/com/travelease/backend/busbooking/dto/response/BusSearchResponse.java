package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.BusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusSearchResponse {

    private Long scheduleId;
    private String busName;
    private String busNumber;
    private BusType busType;
    private String source;
    private String destination;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private Double fare;
    private Integer availableSeats;
    private Double duration;
    private LocalDate travelDate;
    private List<String> amenities;
}
