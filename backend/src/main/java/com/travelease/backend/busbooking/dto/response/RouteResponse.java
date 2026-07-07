package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.RouteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteResponse {

    private Long id;
    private String source;
    private String destination;
    private Double distanceKm;
    private Double durationHours;
    private RouteStatus status;
    private LocalDateTime createdAt;
}
