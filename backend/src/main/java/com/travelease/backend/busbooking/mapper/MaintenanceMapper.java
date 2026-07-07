package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.response.MaintenanceResponse;
import com.travelease.backend.busbooking.entity.Maintenance;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceMapper {

    public MaintenanceResponse toResponse(Maintenance maintenance) {
        return MaintenanceResponse.builder()
                .id(maintenance.getId())
                .busId(maintenance.getBus().getId())
                .busNumber(maintenance.getBus().getBusNumber())
                .maintenanceType(maintenance.getMaintenanceType())
                .description(maintenance.getDescription())
                .status(maintenance.getStatus())
                .scheduledDate(maintenance.getScheduledDate())
                .completedDate(maintenance.getCompletedDate())
                .cost(maintenance.getCost())
                .nextMaintenanceDate(maintenance.getNextMaintenanceDate())
                .performedBy(maintenance.getPerformedBy())
                .createdAt(maintenance.getCreatedAt())
                .build();
    }
}
