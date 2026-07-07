package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.MaintenanceRequest;
import com.travelease.backend.busbooking.dto.request.MaintenanceStatusTransitionRequest;
import com.travelease.backend.busbooking.dto.response.MaintenanceResponse;
import com.travelease.backend.busbooking.entity.enums.MaintenanceStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MaintenanceService {

    MaintenanceResponse scheduleMaintenance(MaintenanceRequest request);

    MaintenanceResponse updateMaintenance(Long id, MaintenanceRequest request);

    MaintenanceResponse getMaintenanceById(Long id);

    List<MaintenanceResponse> getMaintenanceRecords(Long providerId, Long busId, MaintenanceStatus status, Pageable pageable);

    MaintenanceResponse transitionMaintenance(Long id, MaintenanceStatusTransitionRequest request);

    Double getTotalMaintenanceCost(Long busId);
}
