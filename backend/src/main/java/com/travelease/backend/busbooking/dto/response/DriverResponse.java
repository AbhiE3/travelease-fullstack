package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponse {

    private Long id;
    private Long providerId;
    private String name;
    private String licenseNumber;
    private String phone;
    private String email;
    private DriverStatus status;
    private Integer totalTrips;
    private Double totalDistanceKm;
    private Double rating;
    private Boolean active;
    private LocalDateTime createdAt;
}
