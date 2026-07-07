package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.MaintenanceRequest;
import com.travelease.backend.busbooking.dto.request.MaintenanceStatusTransitionRequest;
import com.travelease.backend.busbooking.dto.response.MaintenanceResponse;
import com.travelease.backend.busbooking.entity.Bus;
import com.travelease.backend.busbooking.entity.Maintenance;
import com.travelease.backend.busbooking.entity.enums.BusStatus;
import com.travelease.backend.busbooking.entity.enums.MaintenanceStatus;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.mapper.MaintenanceMapper;
import com.travelease.backend.busbooking.repository.BusRepository;
import com.travelease.backend.busbooking.repository.MaintenanceRepository;
import com.travelease.backend.busbooking.service.MaintenanceService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final BusRepository busRepository;
    private final MaintenanceMapper maintenanceMapper;

    @Override
    @Transactional
    public MaintenanceResponse scheduleMaintenance(MaintenanceRequest request) {
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + request.getBusId()));

        Maintenance maintenance = Maintenance.builder()
                .bus(bus)
                .maintenanceType(request.getMaintenanceType())
                .description(request.getDescription())
                .scheduledDate(request.getScheduledDate())
                .nextMaintenanceDate(request.getNextMaintenanceDate())
                .performedBy(request.getPerformedBy())
                .cost(request.getEstimatedCost() != null ? request.getEstimatedCost() : 0.0)
                .build();

        bus.setStatus(BusStatus.MAINTENANCE);
        busRepository.save(bus);

        Maintenance saved = maintenanceRepository.save(maintenance);
        return maintenanceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MaintenanceResponse updateMaintenance(Long id, MaintenanceRequest request) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with id: " + id));

        maintenance.setMaintenanceType(request.getMaintenanceType());
        maintenance.setDescription(request.getDescription());
        maintenance.setScheduledDate(request.getScheduledDate());
        maintenance.setNextMaintenanceDate(request.getNextMaintenanceDate());
        maintenance.setPerformedBy(request.getPerformedBy());

        Maintenance saved = maintenanceRepository.save(maintenance);
        return maintenanceMapper.toResponse(saved);
    }

    @Override
    public MaintenanceResponse getMaintenanceById(Long id) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with id: " + id));
        return maintenanceMapper.toResponse(maintenance);
    }

    @Override
    public List<MaintenanceResponse> getMaintenanceRecords(Long providerId, Long busId, MaintenanceStatus status, Pageable pageable) {
        Specification<Maintenance> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (providerId != null) {
                predicates.add(cb.equal(root.get("bus").get("providerId"), providerId));
            }
            if (busId != null) {
                predicates.add(cb.equal(root.get("bus").get("id"), busId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return maintenanceRepository.findAll(spec, pageable).stream()
                .map(maintenanceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MaintenanceResponse transitionMaintenance(Long id, MaintenanceStatusTransitionRequest request) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with id: " + id));

        MaintenanceStatus targetStatus = request.getStatus();

        switch (targetStatus) {
            case IN_PROGRESS:
                if (maintenance.getStatus() != MaintenanceStatus.SCHEDULED) {
                    throw new IllegalStateException("Maintenance can only be started from SCHEDULED status");
                }
                maintenance.setStatus(MaintenanceStatus.IN_PROGRESS);
                break;

            case COMPLETED:
                if (maintenance.getStatus() != MaintenanceStatus.IN_PROGRESS) {
                    throw new IllegalStateException("Maintenance can only be completed from IN_PROGRESS status");
                }
                maintenance.setStatus(MaintenanceStatus.COMPLETED);
                maintenance.setCost(request.getCost() != null ? request.getCost() : 0.0);
                maintenance.setCompletedDate(request.getCompletedDate() != null ? request.getCompletedDate() : LocalDate.now());

                Bus bus = maintenance.getBus();
                bus.setStatus(BusStatus.ACTIVE);
                busRepository.save(bus);
                break;

            case CANCELLED:
                if (maintenance.getStatus() != MaintenanceStatus.SCHEDULED && maintenance.getStatus() != MaintenanceStatus.IN_PROGRESS) {
                    throw new IllegalStateException("Maintenance can only be cancelled from SCHEDULED or IN_PROGRESS status");
                }
                maintenance.setStatus(MaintenanceStatus.CANCELLED);

                Bus cancelledBus = maintenance.getBus();
                cancelledBus.setStatus(BusStatus.ACTIVE);
                busRepository.save(cancelledBus);
                break;

            default:
                throw new IllegalStateException("Invalid target status: " + targetStatus);
        }

        Maintenance saved = maintenanceRepository.save(maintenance);
        return maintenanceMapper.toResponse(saved);
    }

    @Override
    public Double getTotalMaintenanceCost(Long busId) {
        return maintenanceRepository.sumCompletedCostByBus(busId);
    }
}
