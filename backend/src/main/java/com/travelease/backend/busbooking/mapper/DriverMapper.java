package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.request.DriverRequest;
import com.travelease.backend.busbooking.dto.response.DriverResponse;
import com.travelease.backend.busbooking.entity.Driver;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {

    public DriverResponse toResponse(Driver driver) {
        return DriverResponse.builder()
                .id(driver.getId())
                .providerId(driver.getProviderId())
                .name(driver.getName())
                .licenseNumber(driver.getLicenseNumber())
                .phone(driver.getPhone())
                .email(driver.getEmail())
                .status(driver.getStatus())
                .totalTrips(driver.getTotalTrips())
                .totalDistanceKm(driver.getTotalDistanceKm())
                .rating(driver.getRating())
                .active(driver.getActive())
                .createdAt(driver.getCreatedAt())
                .build();
    }

    public Driver toEntity(DriverRequest request) {
        return Driver.builder()
                .providerId(request.getProviderId())
                .name(request.getName())
                .licenseNumber(request.getLicenseNumber())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();
    }
}
