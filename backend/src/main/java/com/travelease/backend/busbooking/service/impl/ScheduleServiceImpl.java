package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.BusSearchCriteriaRequest;
import com.travelease.backend.busbooking.dto.request.ScheduleRequest;
import com.travelease.backend.busbooking.dto.response.*;
import com.travelease.backend.busbooking.entity.*;
import com.travelease.backend.busbooking.entity.enums.*;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.mapper.ScheduleMapper;
import com.travelease.backend.busbooking.repository.*;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.busbooking.service.ScheduleService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    private final BusScheduleRepository scheduleRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final BookingRepository bookingRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final ScheduleMapper scheduleMapper;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request) {
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", request.getBusId()));
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", request.getRouteId()));
        ensureAssignable(bus, route);

        BusSchedule schedule = BusSchedule.builder()
                .bus(bus)
                .route(route)
                .travelDate(request.getTravelDate())
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .fare(request.getFare())
                .availableSeats(bus.getTotalSeats())
                .status(ScheduleStatus.SCHEDULED)
                .build();

        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public ScheduleResponse updateSchedule(Long id, ScheduleRequest request) {
        BusSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", request.getBusId()));
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", request.getRouteId()));
        ensureAssignable(bus, route);

        schedule.setBus(bus);
        schedule.setRoute(route);
        schedule.setTravelDate(request.getTravelDate());
        schedule.setDepartureTime(request.getDepartureTime());
        schedule.setArrivalTime(request.getArrivalTime());
        schedule.setFare(request.getFare());
        schedule.setAvailableSeats(bus.getTotalSeats());
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        BusSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));
        schedule.setStatus(ScheduleStatus.CANCELLED);
        scheduleRepository.save(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleResponse getScheduleById(Long id) {
        BusSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));
        return scheduleMapper.toResponse(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAllSchedules() {
        return scheduleRepository.findAll()
                .stream()
                .map(scheduleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusSearchResponse> searchBuses(String source, String destination, LocalDate travelDate) {
        return scheduleRepository.findBySourceDestinationAndDate(
                        source,
                        destination,
                        travelDate,
                        ScheduleStatus.CANCELLED,
                        BusStatus.ACTIVE,
                        RouteStatus.ACTIVE)
                .stream()
                .map(scheduleMapper::toSearchResponse)
                .toList();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Phase 4 â€“ Smart Search & Traveller Discovery
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(readOnly = true)
    public PaginatedSearchResponse<SmartSearchResponse> smartSearch(BusSearchCriteriaRequest criteria) {
        validateSearchCriteria(criteria);

        // Build dynamic specification
        Specification<BusSchedule> spec = buildSearchSpecification(criteria);

        // Build pageable with sorting
        Sort sort = buildSort(criteria.getSortBy());
        PageRequest pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        // Execute query
        Page<BusSchedule> schedulePage = scheduleRepository.findAll(spec, pageable);

        // Map to SmartSearchResponse
        List<SmartSearchResponse> content = schedulePage.getContent().stream()
                .map(scheduleMapper::toSmartSearchResponse)
                .toList();

        // Post-filter by amenities if requested (ElementCollection cannot be filtered via Specification cleanly)
        if (criteria.getAmenities() != null && !criteria.getAmenities().isEmpty()) {
            Set<String> requested = new HashSet<>(criteria.getAmenities());
            content = content.stream()
                    .filter(r -> r.getAmenities() != null && r.getAmenities().containsAll(requested))
                    .collect(Collectors.toList());
        }

        // Record search history (non-blocking for the response)
        recordSearchHistory(criteria);

        return PaginatedSearchResponse.<SmartSearchResponse>builder()
                .content(content)
                .page(schedulePage.getNumber())
                .size(schedulePage.getSize())
                .totalElements(schedulePage.getTotalElements())
                .totalPages(schedulePage.getTotalPages())
                .last(schedulePage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PopularRouteResponse> getPopularRoutes(int limit) {
        List<Object[]> results = searchHistoryRepository.findPopularRoutes(PageRequest.of(0, limit));
        return results.stream()
                .map(row -> PopularRouteResponse.builder()
                        .source((String) row[0])
                        .destination((String) row[1])
                        .searchCount(((Number) row[2]).longValue())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getSearchHistory(Pageable pageable) {
        java.util.UUID userId = securityUtil.getCurrentUserId();
        return searchHistoryRepository.findByUserId(userId, pageable).stream()
                .map(this::mapSearchHistory)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchSuggestionResponse> getSearchSuggestions(int limit) {
        java.util.UUID userId = securityUtil.getCurrentUserId();
        List<Object[]> results = bookingRepository.findSearchSuggestionsByUserId(userId, PageRequest.of(0, limit));
        return results.stream()
                .map(row -> SearchSuggestionResponse.builder()
                        .routeId(((Number) row[2]).longValue())
                        .source((String) row[0])
                        .destination((String) row[1])
                        .bookingCount(((Number) row[3]).longValue())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PopularRouteResponse> getFrequentlyBookedRoutes(int limit) {
        List<Object[]> results = bookingRepository.findFrequentlyBookedRoutes(PageRequest.of(0, limit));
        return results.stream()
                .map(row -> PopularRouteResponse.builder()
                        .routeId(((Number) row[2]).longValue())
                        .source((String) row[0])
                        .destination((String) row[1])
                        .searchCount(((Number) row[3]).longValue())
                        .build())
                .toList();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Private helpers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void validateSearchCriteria(BusSearchCriteriaRequest criteria) {
        if (criteria.getSource() == null || criteria.getSource().isBlank()) {
            throw new IllegalArgumentException("Source must not be blank");
        }
        if (criteria.getDestination() == null || criteria.getDestination().isBlank()) {
            throw new IllegalArgumentException("Destination must not be blank");
        }
        if (criteria.getTravelDate() == null) {
            throw new IllegalArgumentException("Travel date is required");
        }
        if (criteria.getTravelDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Travel date cannot be in the past");
        }
        if (criteria.getMinFare() != null && criteria.getMaxFare() != null
                && criteria.getMinFare() > criteria.getMaxFare()) {
            throw new IllegalArgumentException("Minimum fare cannot be greater than maximum fare");
        }
        if (criteria.getDepartureTimeFrom() != null && criteria.getDepartureTimeTo() != null
                && criteria.getDepartureTimeFrom().isAfter(criteria.getDepartureTimeTo())) {
            throw new IllegalArgumentException("Departure time 'from' cannot be after 'to'");
        }
        if (criteria.getArrivalTimeFrom() != null && criteria.getArrivalTimeTo() != null
                && criteria.getArrivalTimeFrom().isAfter(criteria.getArrivalTimeTo())) {
            throw new IllegalArgumentException("Arrival time 'from' cannot be after 'to'");
        }
    }

    private Specification<BusSchedule> buildSearchSpecification(BusSearchCriteriaRequest criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<BusSchedule, Route> route = root.join("route", JoinType.INNER);
            Join<BusSchedule, Bus> bus = root.join("bus", JoinType.INNER);

            // Required filters
            predicates.add(cb.equal(cb.lower(route.get("source")), criteria.getSource().toLowerCase()));
            predicates.add(cb.equal(cb.lower(route.get("destination")), criteria.getDestination().toLowerCase()));
            predicates.add(cb.equal(root.get("travelDate"), criteria.getTravelDate()));
            predicates.add(cb.notEqual(root.get("status"), ScheduleStatus.CANCELLED));
            predicates.add(cb.equal(bus.get("status"), BusStatus.ACTIVE));
            predicates.add(cb.equal(route.get("status"), RouteStatus.ACTIVE));

            // Optional: bus type
            if (criteria.getBusType() != null) {
                predicates.add(cb.equal(bus.get("busType"), criteria.getBusType()));
            }

            // Optional: AC / Non-AC
            if (criteria.getAc() != null) {
                if (Boolean.TRUE.equals(criteria.getAc())) {
                    predicates.add(bus.get("busType").in(
                            BusType.AC_SLEEPER, BusType.AC_SEMI_SLEEPER, BusType.AC_SEATER, BusType.AC_LUXURY));
                } else {
                    predicates.add(bus.get("busType").in(
                            BusType.NON_AC_SLEEPER, BusType.NON_AC_SEMI_SLEEPER, BusType.NON_AC_SEATER, BusType.NON_AC_LUXURY));
                }
            }

            // Optional: category (SLEEPER, SEMI_SLEEPER, SEATER, LUXURY)
            if (criteria.getCategory() != null && !criteria.getCategory().isBlank()) {
                switch (criteria.getCategory().toUpperCase()) {
                    case "SLEEPER" -> predicates.add(bus.get("busType").in(BusType.AC_SLEEPER, BusType.NON_AC_SLEEPER));
                    case "SEMI_SLEEPER" -> predicates.add(bus.get("busType").in(BusType.AC_SEMI_SLEEPER, BusType.NON_AC_SEMI_SLEEPER));
                    case "SEATER" -> predicates.add(bus.get("busType").in(BusType.AC_SEATER, BusType.NON_AC_SEATER));
                    case "LUXURY" -> predicates.add(bus.get("busType").in(BusType.AC_LUXURY, BusType.NON_AC_LUXURY));
                    default -> { /* ignore unknown category */ }
                }
            }

            // Optional: available seats
            if (criteria.getMinAvailableSeats() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("availableSeats"), criteria.getMinAvailableSeats()));
            }

            // Optional: departure time range
            if (criteria.getDepartureTimeFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("departureTime"), criteria.getDepartureTimeFrom()));
            }
            if (criteria.getDepartureTimeTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("departureTime"), criteria.getDepartureTimeTo()));
            }

            // Optional: arrival time range
            if (criteria.getArrivalTimeFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("arrivalTime"), criteria.getArrivalTimeFrom()));
            }
            if (criteria.getArrivalTimeTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("arrivalTime"), criteria.getArrivalTimeTo()));
            }

            // Optional: fare range
            if (criteria.getMinFare() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fare"), criteria.getMinFare()));
            }
            if (criteria.getMaxFare() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fare"), criteria.getMaxFare()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "departureTime");
        }
        return switch (sortBy.toUpperCase()) {
            case "LOWEST_FARE" -> Sort.by(Sort.Direction.ASC, "fare");
            case "HIGHEST_FARE" -> Sort.by(Sort.Direction.DESC, "fare");
            case "EARLIEST_DEPARTURE" -> Sort.by(Sort.Direction.ASC, "departureTime");
            case "LATEST_DEPARTURE" -> Sort.by(Sort.Direction.DESC, "departureTime");
            case "FASTEST_JOURNEY" -> {
                // Sort by route duration ascending (requires join, handled via schedule's route)
                yield Sort.by(Sort.Direction.ASC, "route.durationHours");
            }
            default -> Sort.by(Sort.Direction.ASC, "departureTime");
        };
    }

    private void recordSearchHistory(BusSearchCriteriaRequest criteria) {
        try {
            java.util.UUID userId = securityUtil.getCurrentUserId();
            SearchHistory history = SearchHistory.builder()
                    .userId(userId)
                    .source(criteria.getSource())
                    .destination(criteria.getDestination())
                    .travelDate(criteria.getTravelDate())
                    .build();
            searchHistoryRepository.save(history);
        } catch (Exception e) {
            // Non-critical: log but don't fail the search
            log.warn("Could not record search history: {}", e.getMessage());
        }
    }

    private SearchHistoryResponse mapSearchHistory(SearchHistory sh) {
        return SearchHistoryResponse.builder()
                .id(sh.getId())
                .source(sh.getSource())
                .destination(sh.getDestination())
                .travelDate(sh.getTravelDate())
                .searchedAt(sh.getSearchedAt())
                .build();
    }

    private void ensureAssignable(Bus bus, Route route) {
        if (bus.getStatus() != BusStatus.ACTIVE) {
            throw new IllegalStateException("Bus is not active");
        }
        if (route.getStatus() != RouteStatus.ACTIVE) {
            throw new IllegalStateException("Route is not active");
        }
    }
}
