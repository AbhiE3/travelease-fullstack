package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.ReportFilterRequest;
import com.travelease.backend.busbooking.dto.response.*;
import com.travelease.backend.busbooking.entity.*;
import com.travelease.backend.busbooking.entity.enums.*;
import com.travelease.backend.busbooking.repository.*;
import com.travelease.backend.busbooking.service.AnalyticsService;
import com.travelease.backend.busbooking.service.ReportService;
import com.travelease.backend.busbooking.util.ReportExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final BookingRepository bookingRepository;
    private final BusRepository busRepository;
    private final BusScheduleRepository scheduleRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final ConductorRepository conductorRepository;
    private final TripRepository tripRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final RefundRepository refundRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final AnalyticsService analyticsService;
    private final ReportExportUtil reportExportUtil;

    private static final java.util.Set<BookingStatus> SUCCESSFUL_BOOKING_STATUSES =
            java.util.EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
    private static final java.util.Set<BookingStatus> CANCELLED_BOOKING_STATUSES =
            java.util.EnumSet.of(BookingStatus.CANCELLED);

    private void validateBookingStatusFilter(java.util.Set<BookingStatus> validStatuses, BookingStatus requested, String reportLabel) {
        if (requested != null && !validStatuses.contains(requested)) {
            throw new IllegalArgumentException(
                    reportLabel + " only supports bookingStatus in " + validStatuses + ", got " + requested);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse generateReport(ReportType reportType, ReportFilterRequest filters) {
        return switch (reportType) {
            case BOOKING -> generateBookingReport(filters);
            case REVENUE -> generateRevenueReport(filters);
            case PASSENGER -> generatePassengerReport(filters);
            case BUS_PERFORMANCE -> generateBusPerformanceReport(filters);
            case ROUTE_PERFORMANCE -> generateRoutePerformanceReport(filters);
            case DRIVER_PERFORMANCE -> generateDriverPerformanceReport(filters);
            case CONDUCTOR_PERFORMANCE -> generateConductorPerformanceReport(filters);
            case FLEET_UTILIZATION -> generateFleetUtilizationReport(filters);
            case MAINTENANCE -> generateMaintenanceReport(filters);
            case REFUND -> generateRefundReport(filters);
            case CANCELLATION -> generateCancellationReport(filters);
        };
    }

    @Override
    public ReportResponse generateBookingReport(ReportFilterRequest filters) {
        List<Booking> bookings = getFilteredBookings(filters);
        List<Map<String, Object>> data = bookings.stream()
                .map(this::bookingToMap)
                .collect(Collectors.toList());

        double totalRevenue = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
                .mapToDouble(b -> b.getTotalFare() != null ? b.getTotalFare() : 0.0)
                .sum();

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRecords((long) bookings.size())
                .totalRevenue(totalRevenue)
                .totalBookings((long) bookings.size())
                .totalPassengers(bookings.stream()
                        .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
                        .mapToLong(b -> b.getBookingSeats() != null ? b.getBookingSeats().size() : 1)
                        .sum())
                .totalCancellations(bookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count())
                .build();

        return buildReportResponse("Booking Report", "BOOKING", data, summary, filters, bookings.size());
    }

    @Override
    public ReportResponse generateRevenueReport(ReportFilterRequest filters) {
        validateBookingStatusFilter(SUCCESSFUL_BOOKING_STATUSES, filters.getBookingStatus(), "Revenue report");
        java.util.Set<BookingStatus> effectiveStatuses = filters.getBookingStatus() != null
                ? java.util.EnumSet.of(filters.getBookingStatus())
                : SUCCESSFUL_BOOKING_STATUSES;
        List<Booking> bookings = getFilteredBookings(filters).stream()
                .filter(b -> effectiveStatuses.contains(b.getStatus()))
                .collect(Collectors.toList());

        List<Map<String, Object>> data = bookings.stream()
                .map(b -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("bookingId", b.getId());
                    map.put("bookingReference", b.getBookingReference());
                    map.put("busNumber", b.getSchedule().getBus().getBusNumber());
                    map.put("route", b.getSchedule().getRoute().getSource() + " → " + b.getSchedule().getRoute().getDestination());
                    map.put("travelDate", b.getSchedule().getTravelDate().toString());
                    map.put("fare", b.getTotalFare());
                    map.put("couponCode", b.getCouponCode());
                    map.put("couponDiscount", b.getCouponDiscount());
                    map.put("netRevenue", b.getTotalFare() - (b.getCouponDiscount() != null ? b.getCouponDiscount() : 0.0));
                    map.put("bookedAt", b.getBookedAt().toString());
                    return map;
                })
                .collect(Collectors.toList());

        double totalRevenue = bookings.stream().mapToDouble(b -> b.getTotalFare() != null ? b.getTotalFare() : 0.0).sum();
        double totalDiscount = bookings.stream().mapToDouble(b -> b.getCouponDiscount() != null ? b.getCouponDiscount() : 0.0).sum();

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRecords((long) bookings.size())
                .totalRevenue(totalRevenue - totalDiscount)
                .totalBookings((long) bookings.size())
                .build();

        return buildReportResponse("Revenue Report", "REVENUE", data, summary, filters, bookings.size());
    }

    @Override
    public ReportResponse generatePassengerReport(ReportFilterRequest filters) {
        validateBookingStatusFilter(SUCCESSFUL_BOOKING_STATUSES, filters.getBookingStatus(), "Passenger report");
        java.util.Set<BookingStatus> effectiveStatuses = filters.getBookingStatus() != null
                ? java.util.EnumSet.of(filters.getBookingStatus())
                : SUCCESSFUL_BOOKING_STATUSES;
        List<Booking> bookings = getFilteredBookings(filters).stream()
                .filter(b -> effectiveStatuses.contains(b.getStatus()))
                .collect(Collectors.toList());

        List<Map<String, Object>> data = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getBookingSeats() != null) {
                double perSeatFare = b.getTotalFare() != null && !b.getBookingSeats().isEmpty()
                        ? b.getTotalFare() / b.getBookingSeats().size() : 0.0;
                for (BookingSeat seat : b.getBookingSeats()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("bookingReference", b.getBookingReference());
                    map.put("passengerName", seat.getPassengerName());
                    map.put("seatNumber", seat.getSeat() != null ? seat.getSeat().getSeatNumber() : "N/A");
                    map.put("busNumber", b.getSchedule().getBus().getBusNumber());
                    map.put("route", b.getSchedule().getRoute().getSource() + " → " + b.getSchedule().getRoute().getDestination());
                    map.put("travelDate", b.getSchedule().getTravelDate().toString());
                    map.put("fare", perSeatFare);
                    data.add(map);
                }
            }
        }

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRecords((long) data.size())
                .totalPassengers((long) data.size())
                .totalBookings((long) bookings.size())
                .build();

        return buildReportResponse("Passenger Report", "PASSENGER", data, summary, filters, data.size());
    }

    @Override
    public ReportResponse generateBusPerformanceReport(ReportFilterRequest filters) {
        List<BusAnalyticsResponse> analytics = analyticsService.getBusAnalytics(filters.getProviderId());
        List<Map<String, Object>> data = analytics.stream()
                .map(a -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("busId", a.getBusId());
                    map.put("busNumber", a.getBusNumber());
                    map.put("busName", a.getBusName());
                    map.put("utilizationPercent", a.getUtilizationPercentage());
                    map.put("occupancyPercent", a.getOccupancyPercentage());
                    map.put("revenue", a.getRevenue());
                    map.put("tripCount", a.getTripCount());
                    map.put("bookingCount", a.getBookingCount());
                    map.put("seatsSold", a.getSeatsSold());
                    map.put("performance", a.getPerformanceCategory());
                    return map;
                })
                .collect(Collectors.toList());

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRecords((long) analytics.size())
                .totalRevenue(analytics.stream().mapToDouble(a -> a.getRevenue() != null ? a.getRevenue() : 0).sum())
                .build();

        return buildReportResponse("Bus Performance Report", "BUS_PERFORMANCE", data, summary, filters, analytics.size());
    }

    @Override
    public ReportResponse generateRoutePerformanceReport(ReportFilterRequest filters) {
        List<RouteAnalyticsResponse> analytics = analyticsService.getRouteAnalytics(filters.getProviderId());
        List<Map<String, Object>> data = analytics.stream()
                .map(a -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("routeId", a.getRouteId());
                    map.put("route", a.getSource() + " → " + a.getDestination());
                    map.put("revenue", a.getRevenue());
                    map.put("bookingCount", a.getBookingCount());
                    map.put("passengerCount", a.getPassengerCount());
                    map.put("occupancyPercent", a.getOccupancyPercentage());
                    map.put("distanceKm", a.getDistanceKm());
                    map.put("revenuePerKm", a.getRevenuePerKm());
                    map.put("performance", a.getPerformanceCategory());
                    return map;
                })
                .collect(Collectors.toList());

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRecords((long) analytics.size())
                .totalRevenue(analytics.stream().mapToDouble(a -> a.getRevenue() != null ? a.getRevenue() : 0).sum())
                .totalBookings(analytics.stream().mapToLong(a -> a.getBookingCount() != null ? a.getBookingCount() : 0).sum())
                .build();

        return buildReportResponse("Route Performance Report", "ROUTE_PERFORMANCE", data, summary, filters, analytics.size());
    }

    @Override
    public ReportResponse generateDriverPerformanceReport(ReportFilterRequest filters) {
        List<DriverAnalyticsResponse> analytics = analyticsService.getDriverAnalytics(filters.getProviderId()).stream()
                .sorted((a, b) -> {
                    int cmp = Long.compare(b.getCompletedTrips() != null ? b.getCompletedTrips() : 0, a.getCompletedTrips() != null ? a.getCompletedTrips() : 0);
                    if (cmp != 0) return cmp;
                    return Double.compare(b.getRating() != null ? b.getRating() : 0, a.getRating() != null ? a.getRating() : 0);
                })
                .collect(Collectors.toList());
        List<Map<String, Object>> data = analytics.stream()
                .map(a -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("driverId", a.getDriverId());
                    map.put("driverName", a.getDriverName());
                    map.put("licenseNumber", a.getLicenseNumber());
                    map.put("totalTrips", a.getTotalTrips());
                    map.put("completedTrips", a.getCompletedTrips());
                    map.put("distanceCovered", a.getDistanceCovered());
                    map.put("rating", a.getRating());
                    map.put("utilizationPercent", a.getUtilizationPercentage());
                    map.put("performance", a.getPerformanceCategory());
                    return map;
                })
                .collect(Collectors.toList());

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRecords((long) analytics.size())
                .build();

        return buildReportResponse("Driver Performance Report", "DRIVER_PERFORMANCE", data, summary, filters, analytics.size());
    }

    @Override
    public ReportResponse generateConductorPerformanceReport(ReportFilterRequest filters) {
        List<ConductorAnalyticsResponse> analytics = analyticsService.getConductorAnalytics(filters.getProviderId()).stream()
                .sorted((a, b) -> {
                    int cmp = Long.compare(b.getCompletedTrips() != null ? b.getCompletedTrips() : 0, a.getCompletedTrips() != null ? a.getCompletedTrips() : 0);
                    if (cmp != 0) return cmp;
                    return Double.compare(b.getRating() != null ? b.getRating() : 0, a.getRating() != null ? a.getRating() : 0);
                })
                .collect(Collectors.toList());
        List<Map<String, Object>> data = analytics.stream()
                .map(a -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("conductorId", a.getConductorId());
                    map.put("conductorName", a.getConductorName());
                    map.put("employeeId", a.getEmployeeId());
                    map.put("totalTrips", a.getTotalTrips());
                    map.put("completedTrips", a.getCompletedTrips());
                    map.put("passengerHandling", a.getPassengerHandling());
                    map.put("rating", a.getRating());
                    map.put("performance", a.getPerformanceCategory());
                    return map;
                })
                .collect(Collectors.toList());

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRecords((long) analytics.size())
                .build();

        return buildReportResponse("Conductor Performance Report", "CONDUCTOR_PERFORMANCE", data, summary, filters, analytics.size());
    }

    @Override
    public ReportResponse generateFleetUtilizationReport(ReportFilterRequest filters) {
        List<Bus> buses = busRepository.findByProviderId(filters.getProviderId());
        List<Map<String, Object>> data = buses.stream()
                .map(b -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("busId", b.getId());
                    map.put("busNumber", b.getBusNumber());
                    map.put("busName", b.getBusName());
                    map.put("status", b.getStatus().name());
                    map.put("totalSeats", b.getTotalSeats());
                    Long seatsSold = bookingRepository.countSeatsSoldByBus(b.getId());
                    double utilization = b.getTotalSeats() > 0 && seatsSold > 0
                            ? Math.min((seatsSold * 100.0 / b.getTotalSeats()), 100.0)
                            : 0.0;
                    map.put("seatsSold", seatsSold);
                    map.put("utilizationPercent", utilization);
                    return map;
                })
                .collect(Collectors.toList());

        long activeBuses = buses.stream().filter(b -> b.getStatus() == BusStatus.ACTIVE).count();
        double avgUtilization = data.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("utilizationPercent", 0.0))
                .average().orElse(0.0);

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRecords((long) buses.size())
                .fleetUtilization(avgUtilization)
                .build();

        return buildReportResponse("Fleet Utilization Report", "FLEET_UTILIZATION", data, summary, filters, buses.size());
    }

    @Override
    public ReportResponse generateMaintenanceReport(ReportFilterRequest filters) {
        // Provider-scoped first (when present) so a busId filter on top can never
        // surface another provider's bus - the busId simply won't match anything
        // in an already provider-scoped list.
        List<Maintenance> maintenanceList = filters.getProviderId() != null
                ? maintenanceRepository.findByProviderId(filters.getProviderId())
                : maintenanceRepository.findAll();
        if (filters.getBusId() != null) {
            maintenanceList = maintenanceList.stream()
                    .filter(m -> m.getBus().getId().equals(filters.getBusId()))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> data = maintenanceList.stream()
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("maintenanceId", m.getId());
                    map.put("busNumber", m.getBus().getBusNumber());
                    map.put("maintenanceType", m.getMaintenanceType());
                    map.put("description", m.getDescription());
                    map.put("status", m.getStatus().name());
                    map.put("scheduledDate", m.getScheduledDate().toString());
                    map.put("completedDate", m.getCompletedDate() != null ? m.getCompletedDate().toString() : "N/A");
                    map.put("cost", m.getCost());
                    map.put("performedBy", m.getPerformedBy());
                    return map;
                })
                .collect(Collectors.toList());

        double totalCost = maintenanceList.stream()
                .filter(m -> m.getStatus() == MaintenanceStatus.COMPLETED)
                .mapToDouble(m -> m.getCost() != null ? m.getCost() : 0.0)
                .sum();

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRecords((long) maintenanceList.size())
                .totalRevenue(totalCost)
                .build();

        return buildReportResponse("Maintenance Report", "MAINTENANCE", data, summary, filters, maintenanceList.size());
    }

    @Override
    public ReportResponse generateRefundReport(ReportFilterRequest filters) {
        List<Refund> refunds = filters.getProviderId() != null
                ? refundRepository.findByBooking_Schedule_Bus_ProviderId(filters.getProviderId())
                : refundRepository.findAll();

        List<Map<String, Object>> data = refunds.stream()
                .map(r -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("refundId", r.getId());
                    map.put("refundReference", r.getRefundReference());
                    map.put("bookingReference", r.getBooking().getBookingReference());
                    map.put("originalAmount", r.getOriginalAmount());
                    map.put("cancellationCharge", r.getCancellationCharge());
                    map.put("netRefundable", r.getNetRefundable());
                    map.put("status", r.getStatus().name());
                    map.put("initiatedAt", r.getInitiatedAt().toString());
                    map.put("completedAt", r.getCompletedAt() != null ? r.getCompletedAt().toString() : "N/A");
                    return map;
                })
                .collect(Collectors.toList());

        double totalRefund = refunds.stream()
                .filter(r -> r.getStatus() == RefundStatus.COMPLETED)
                .mapToDouble(r -> r.getNetRefundable() != null ? r.getNetRefundable() : 0.0)
                .sum();

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRecords((long) refunds.size())
                .totalRefunds((long) refunds.size())
                .totalRevenue(totalRefund)
                .build();

        return buildReportResponse("Refund Report", "REFUND", data, summary, filters, refunds.size());
    }

    @Override
    public ReportResponse generateCancellationReport(ReportFilterRequest filters) {
        validateBookingStatusFilter(CANCELLED_BOOKING_STATUSES, filters.getBookingStatus(), "Cancellation report");
        List<Booking> cancelledBookings = getFilteredBookings(filters).stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .collect(Collectors.toList());

        List<Map<String, Object>> data = cancelledBookings.stream()
                .map(b -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("bookingId", b.getId());
                    map.put("bookingReference", b.getBookingReference());
                    map.put("busNumber", b.getSchedule().getBus().getBusNumber());
                    map.put("route", b.getSchedule().getRoute().getSource() + " → " + b.getSchedule().getRoute().getDestination());
                    map.put("travelDate", b.getSchedule().getTravelDate().toString());
                    map.put("totalFare", b.getTotalFare());
                    map.put("refundAmount", b.getTotalRefundAmount());
                    map.put("cancellationReason", b.getCancellationReason() != null ? b.getCancellationReason().name() : "N/A");
                    map.put("reasonText", b.getCancellationReasonText());
                    map.put("cancelledAt", b.getCancelledAt() != null ? b.getCancelledAt().toString() : "N/A");
                    return map;
                })
                .collect(Collectors.toList());

        double totalRefund = cancelledBookings.stream()
                .mapToDouble(b -> b.getTotalRefundAmount() != null ? b.getTotalRefundAmount() : 0.0)
                .sum();

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalRecords((long) cancelledBookings.size())
                .totalCancellations((long) cancelledBookings.size())
                .totalRefunds((long) cancelledBookings.size())
                .totalRevenue(totalRefund)
                .build();

        return buildReportResponse("Cancellation Report", "CANCELLATION", data, summary, filters, cancelledBookings.size());
    }

    @Override
    public byte[] exportReportToExcel(ReportType reportType, ReportFilterRequest filters) {
        ReportResponse report = generateReport(reportType, filters);
        saveReportHistory(reportType, filters, "EXCEL", report.getData().size());
        try {
            return reportExportUtil.generateExcel(report.getReportName(), report.getData());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }

    @Override
    public String exportReportToCsv(ReportType reportType, ReportFilterRequest filters) {
        ReportResponse report = generateReport(reportType, filters);
        saveReportHistory(reportType, filters, "CSV", report.getData().size());
        return reportExportUtil.generateCsv(report.getReportName(), report.getData());
    }

    @Override
    public List<ReportHistoryResponse> getReportHistory(Long providerId, ReportType reportType, LocalDate from, LocalDate to, org.springframework.data.domain.Pageable pageable) {
        Specification<ReportHistory> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (providerId != null) {
                predicates.add(cb.equal(root.get("providerId"), providerId));
            }
            if (reportType != null) {
                predicates.add(cb.equal(root.get("reportType"), reportType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("generatedAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("generatedAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return reportHistoryRepository.findAll(spec, pageable).stream()
                .map(this::historyToResponse)
                .collect(Collectors.toList());
    }

    public List<ReportHistoryResponse> getRecentReports() {
        return reportHistoryRepository.findTop10ByOrderByGeneratedAtDesc().stream()
                .map(this::historyToResponse)
                .collect(Collectors.toList());
    }

    // ==================== Helper Methods ====================

    // Fix C-2: Use JPA Specification to push all filtering to the database
    private List<Booking> getFilteredBookings(ReportFilterRequest filters) {
        Specification<Booking> spec = Specification.where(null);

        if (filters.getProviderId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.join("schedule").join("bus").get("providerId"), filters.getProviderId()));
        }
        if (filters.getBusId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.join("schedule").join("bus").get("id"), filters.getBusId()));
        }
        if (filters.getRouteId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.join("schedule").join("route").get("id"), filters.getRouteId()));
        }
        if (filters.getBookingStatus() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), filters.getBookingStatus()));
        }
        if (filters.getStartDate() != null) {
            LocalDateTime startDateTime = filters.getStartDate().atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("bookedAt"), startDateTime));
        }
        if (filters.getEndDate() != null) {
            LocalDateTime endDateTime = filters.getEndDate().plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("bookedAt"), endDateTime));
        }

        return bookingRepository.findAll(spec);
    }

    private Map<String, Object> bookingToMap(Booking b) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("bookingId", b.getId());
        map.put("bookingReference", b.getBookingReference());
        map.put("userId", b.getUserId());
        map.put("busNumber", b.getSchedule().getBus().getBusNumber());
        map.put("route", b.getSchedule().getRoute().getSource() + " → " + b.getSchedule().getRoute().getDestination());
        map.put("travelDate", b.getSchedule().getTravelDate().toString());
        map.put("status", b.getStatus().name());
        map.put("totalFare", b.getTotalFare());
        map.put("seats", b.getBookingSeats() != null ? b.getBookingSeats().size() : 0);
        map.put("bookedAt", b.getBookedAt() != null ? b.getBookedAt().toString() : "N/A");
        return map;
    }

    private ReportResponse buildReportResponse(String name, String type, List<Map<String, Object>> data,
                                                ReportSummaryResponse summary, ReportFilterRequest filters, int totalSize) {
        Map<String, Object> appliedFilters = new LinkedHashMap<>();
        if (filters.getProviderId() != null) appliedFilters.put("providerId", filters.getProviderId());
        if (filters.getBusId() != null) appliedFilters.put("busId", filters.getBusId());
        if (filters.getRouteId() != null) appliedFilters.put("routeId", filters.getRouteId());
        if (filters.getStartDate() != null) appliedFilters.put("startDate", filters.getStartDate().toString());
        if (filters.getEndDate() != null) appliedFilters.put("endDate", filters.getEndDate().toString());
        if (filters.getBookingStatus() != null) appliedFilters.put("bookingStatus", filters.getBookingStatus().name());

        return ReportResponse.builder()
                .reportName(name)
                .reportType(type)
                .generatedAt(LocalDateTime.now())
                .generatedBy("SYSTEM")
                .summary(summary)
                .data(data)
                .page(filters.getPage())
                .size(filters.getSize())
                .totalRecords(totalSize)
                .totalPages((int) Math.ceil((double) totalSize / filters.getSize()))
                .appliedFilters(appliedFilters)
                .build();
    }

    private ReportHistoryResponse historyToResponse(ReportHistory h) {
        return ReportHistoryResponse.builder()
                .id(h.getId())
                .reportName(h.getReportName())
                .reportType(h.getReportType().name())
                .generatedAt(h.getGeneratedAt())
                .generatedBy(h.getGeneratedBy())
                .appliedFilters(h.getAppliedFilters())
                .exportFormat(h.getExportFormat())
                .recordCount(h.getRecordCount())
                .providerId(h.getProviderId())
                .build();
    }

    private void saveReportHistory(ReportType reportType, ReportFilterRequest filters, String format, int recordCount) {
        String filterStr = String.format("provider=%s, bus=%s, route=%s, start=%s, end=%s",
                filters.getProviderId(), filters.getBusId(), filters.getRouteId(),
                filters.getStartDate(), filters.getEndDate());

        ReportHistory history = ReportHistory.builder()
                .reportName(reportType.name() + " Report")
                .reportType(reportType)
                .generatedBy("SYSTEM")
                .appliedFilters(filterStr)
                .exportFormat(format)
                .recordCount(recordCount)
                .providerId(filters.getProviderId())
                .build();

        reportHistoryRepository.save(history);
    }
}
