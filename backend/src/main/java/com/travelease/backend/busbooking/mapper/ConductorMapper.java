package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.request.ConductorRequest;
import com.travelease.backend.busbooking.dto.response.ConductorResponse;
import com.travelease.backend.busbooking.entity.Conductor;
import org.springframework.stereotype.Component;

@Component
public class ConductorMapper {

    public ConductorResponse toResponse(Conductor conductor) {
        return ConductorResponse.builder()
                .id(conductor.getId())
                .providerId(conductor.getProviderId())
                .name(conductor.getName())
                .employeeId(conductor.getEmployeeId())
                .phone(conductor.getPhone())
                .email(conductor.getEmail())
                .status(conductor.getStatus())
                .totalTrips(conductor.getTotalTrips())
                .rating(conductor.getRating())
                .active(conductor.getActive())
                .createdAt(conductor.getCreatedAt())
                .build();
    }

    public Conductor toEntity(ConductorRequest request) {
        return Conductor.builder()
                .providerId(request.getProviderId())
                .name(request.getName())
                .employeeId(request.getEmployeeId())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();
    }
}
