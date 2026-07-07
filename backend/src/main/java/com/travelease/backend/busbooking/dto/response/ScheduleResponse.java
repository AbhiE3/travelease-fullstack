package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {

    private Long id;
    private BusResponse bus;
    private RouteResponse route;
    private LocalDate travelDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private Double fare;
    private Integer availableSeats;
    private ScheduleStatus status;
}
