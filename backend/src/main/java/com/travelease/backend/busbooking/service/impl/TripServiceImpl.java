package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.TripAssignmentRequest;
import com.travelease.backend.busbooking.dto.request.TripStatusTransitionRequest;
import com.travelease.backend.busbooking.dto.response.FleetAvailabilityResponse;
import com.travelease.backend.busbooking.dto.response.TripResponse;
import com.travelease.backend.busbooking.entity.*;
import com.travelease.backend.busbooking.entity.enums.*;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.mapper.TripMapper;
import com.travelease.backend.busbooking.repository.*;
import com.travelease.backend.busbooking.service.TripService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final BusScheduleRepository scheduleRepository;
    private final DriverRepository driverRepository;
    private final ConductorRepository conductorRepository;
    private final BusRepository busRepository;
    private final TripMapper tripMapper;

    @Override
    @Transactional
    public TripResponse assignTrip(TripAssignmentRequest request) {
        BusSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + request.getScheduleId()));

        if (schedule.getStatus() != ScheduleStatus.SCHEDULED) {
            throw new IllegalStateException("Schedule is not active");
        }

        Bus bus = schedule.getBus();
        if (bus.getStatus() != BusStatus.ACTIVE) {
            throw new IllegalStateException("Bus is not available for trip assignment");
        }

        Driver driver = null;
        if (request.getDriverId() != null) {
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.getDriverId()));

            if (driver.getStatus() != DriverStatus.AVAILABLE) {
                throw new IllegalStateException("Driver is not available");
            }

            driver.setStatus(DriverStatus.ASSIGNED);
            driverRepository.save(driver);
        }

        Conductor conductor = null;
        if (request.getConductorId() != null) {
            conductor = conductorRepository.findById(request.getConductorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conductor not found with id: " + request.getConductorId()));

            if (conductor.getStatus() != ConductorStatus.AVAILABLE) {
                throw new IllegalStateException("Conductor is not available");
            }

            conductor.setStatus(ConductorStatus.ASSIGNED);
            conductorRepository.save(conductor);
        }

        Trip trip = Trip.builder()
                .schedule(schedule)
                .driver(driver)
                .conductor(conductor)
                .notes(request.getNotes())
                .build();

        Trip saved = tripRepository.save(trip);
        return tripMapper.toResponse(saved);
    }

    @Override
    public TripResponse getTripById(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + id));
        return tripMapper.toResponse(trip);
    }

    @Override
    public List<TripResponse> getTrips(Long scheduleId, Long driverId, Long conductorId, TripStatus status, Pageable pageable) {
        Specification<Trip> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (scheduleId != null) {
                predicates.add(cb.equal(root.get("schedule").get("id"), scheduleId));
            }
            if (driverId != null) {
                predicates.add(cb.equal(root.get("driver").get("id"), driverId));
            }
            if (conductorId != null) {
                predicates.add(cb.equal(root.get("conductor").get("id"), conductorId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return tripRepository.findAll(spec, pageable).stream()
                .map(tripMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TripResponse transitionTrip(Long id, TripStatusTransitionRequest request) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + id));

        TripStatus targetStatus = request.getStatus();

        switch (targetStatus) {
            case BOARDING:
                if (trip.getStatus() != TripStatus.SCHEDULED) {
                    throw new IllegalStateException("Trip can only start boarding from SCHEDULED status");
                }
                trip.setStatus(TripStatus.BOARDING);
                if (trip.getDriver() != null) {
                    Driver driver = trip.getDriver();
                    driver.setStatus(DriverStatus.ON_TRIP);
                    driverRepository.save(driver);
                }
                if (trip.getConductor() != null) {
                    Conductor conductor = trip.getConductor();
                    conductor.setStatus(ConductorStatus.ON_TRIP);
                    conductorRepository.save(conductor);
                }
                break;

            case DEPARTED:
                if (trip.getStatus() != TripStatus.BOARDING) {
                    throw new IllegalStateException("Trip can only depart from BOARDING status");
                }
                trip.setStatus(TripStatus.DEPARTED);
                trip.setActualDepartureTime(LocalDateTime.now());
                break;

            case RUNNING:
                if (trip.getStatus() != TripStatus.DEPARTED) {
                    throw new IllegalStateException("Trip can only run from DEPARTED status");
                }
                trip.setStatus(TripStatus.RUNNING);
                break;

            case DELAYED:
                if (trip.getStatus() != TripStatus.RUNNING && trip.getStatus() != TripStatus.DEPARTED) {
                    throw new IllegalStateException("Trip can only be marked delayed from RUNNING or DEPARTED status");
                }
                trip.setStatus(TripStatus.DELAYED);
                trip.setDelayMinutes(request.getDelayMinutes() != null ? request.getDelayMinutes() : 0);
                break;

            case ARRIVED:
                if (trip.getStatus() != TripStatus.RUNNING && trip.getStatus() != TripStatus.DELAYED) {
                    throw new IllegalStateException("Trip can only arrive from RUNNING or DELAYED status");
                }
                trip.setStatus(TripStatus.ARRIVED);
                trip.setActualArrivalTime(LocalDateTime.now());
                break;

            case COMPLETED:
                if (trip.getStatus() != TripStatus.ARRIVED) {
                    throw new IllegalStateException("Trip can only be completed from ARRIVED status");
                }
                trip.setStatus(TripStatus.COMPLETED);
                trip.setDistanceCoveredKm(request.getDistanceCoveredKm() != null ? request.getDistanceCoveredKm() : 0.0);
                if (trip.getDriver() != null) {
                    Driver driver = trip.getDriver();
                    driver.setTotalTrips(driver.getTotalTrips() + 1);
                    driver.setTotalDistanceKm(driver.getTotalDistanceKm() + trip.getDistanceCoveredKm());
                    driver.setStatus(DriverStatus.AVAILABLE);
                    driverRepository.save(driver);
                }
                if (trip.getConductor() != null) {
                    Conductor conductor = trip.getConductor();
                    conductor.setTotalTrips(conductor.getTotalTrips() + 1);
                    conductor.setStatus(ConductorStatus.AVAILABLE);
                    conductorRepository.save(conductor);
                }
                break;

            case CANCELLED:
                if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
                    throw new IllegalStateException("Trip cannot be cancelled from " + trip.getStatus() + " status");
                }
                trip.setStatus(TripStatus.CANCELLED);
                if (request.getReason() != null) {
                    trip.setNotes(request.getReason());
                }
                if (trip.getDriver() != null) {
                    Driver driver = trip.getDriver();
                    driver.setStatus(DriverStatus.AVAILABLE);
                    driverRepository.save(driver);
                }
                if (trip.getConductor() != null) {
                    Conductor conductor = trip.getConductor();
                    conductor.setStatus(ConductorStatus.AVAILABLE);
                    conductorRepository.save(conductor);
                }
                break;

            default:
                throw new IllegalStateException("Invalid target status: " + targetStatus);
        }

        Trip saved = tripRepository.save(trip);
        return tripMapper.toResponse(saved);
    }

    @Override
    public FleetAvailabilityResponse getFleetAvailability(Long providerId) {
        List<Bus> buses = busRepository.findByProviderId(providerId);

        long totalBuses = buses.size();
        long activeBuses = buses.stream().filter(b -> b.getStatus() == BusStatus.ACTIVE).count();
        long maintenanceBuses = buses.stream().filter(b -> b.getStatus() == BusStatus.MAINTENANCE).count();
        long inactiveBuses = buses.stream().filter(b -> b.getStatus() == BusStatus.INACTIVE).count();

        long availableDrivers = driverRepository.countActiveByProvider(providerId);
        long availableConductors = conductorRepository.countActiveByProvider(providerId);

        List<Trip> allTrips = tripRepository.findAll();
        long activeTrips = allTrips.stream()
                .filter(t -> t.getSchedule().getBus().getProviderId().equals(providerId))
                .filter(t -> t.getStatus() == TripStatus.RUNNING || t.getStatus() == TripStatus.DEPARTED || t.getStatus() == TripStatus.BOARDING)
                .count();
        long scheduledTrips = allTrips.stream()
                .filter(t -> t.getSchedule().getBus().getProviderId().equals(providerId))
                .filter(t -> t.getStatus() == TripStatus.SCHEDULED)
                .count();

        return FleetAvailabilityResponse.builder()
                .providerId(providerId)
                .totalBuses(totalBuses)
                .activeBuses(activeBuses)
                .maintenanceBuses(maintenanceBuses)
                .inactiveBuses(inactiveBuses)
                .availableDrivers(availableDrivers)
                .availableConductors(availableConductors)
                .activeTrips(activeTrips)
                .scheduledTrips(scheduledTrips)
                .build();
    }
}
