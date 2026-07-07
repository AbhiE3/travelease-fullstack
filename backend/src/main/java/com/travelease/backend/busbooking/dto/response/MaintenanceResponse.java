package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.MaintenanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceResponse {

    private Long id;
    private Long busId;
    private String busNumber;
    private String maintenanceType;
    private String description;
    private MaintenanceStatus status;
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    private Double cost;
    private LocalDate nextMaintenanceDate;
    private String performedBy;
    private LocalDateTime createdAt;
}
