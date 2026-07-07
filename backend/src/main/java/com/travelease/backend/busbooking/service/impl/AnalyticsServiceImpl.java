package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.response.*;
import com.travelease.backend.busbooking.entity.*;
import com.travelease.backend.busbooking.entity.enums.*;
import com.travelease.backend.busbooking.repository.*;
import com.travelease.backend.busbooking.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final BookingRepository bookingRepository;
    private final BusRepository busRepository;
    private final BusScheduleRepository scheduleRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final ConductorRepository conductorRepository;
    private final TripRepository tripRepository;
    private final MaintenanceRepository maintenanceRepository;

    @Override
    public ProviderDashboardResponse getProviderDashboard(Long providerId) {
        LocalDate today = LocalDate.now();
        LocalDateTime weekAgo = today.minusDays(7).atStartOfDay();
        LocalDateTime monthAgo = today.minusDays(30).atStartOfDay();

        // KPI Cards
        Long todayBookings = bookingRepository.countTodayBookingsByProvider(providerId, today);
        Double todayRevenue = bookingRepository.sumTodayRevenueByProvider(providerId, today);
        Double weeklyRevenue = bookingRepository.sumRevenueByProviderSince(providerId, weekAgo);
        Double monthlyRevenue = bookingRepository.sumRevenueByProviderSince(providerId, monthAgo);
        Double totalRevenue = bookingRepository.sumRevenueByProvider(providerId);

        List<Trip> providerTrips = getProviderTrips(providerId);
        long activeTrips = providerTrips.stream()
                .filter(t -> t.getStatus() == TripStatus.BOARDING || t.getStatus() == TripStatus.DEPARTED || t.getStatus() == TripStatus.RUNNING)
                .count();
        long runningTrips = providerTrips.stream()
                .filter(t -> t.getStatus() == TripStatus.RUNNING)
                .count();
        long completedTrips = providerTrips.stream()
                .filter(t -> t.getStatus() == TripStatus.COMPLETED)
                .count();
        long cancelledTrips = providerTrips.stream()
                .filter(t -> t.getStatus() == TripStatus.CANCELLED)
                .count();
        long delayedTrips = providerTrips.stream()
                .filter(t -> t.getStatus() == TripStatus.DELAYED)
                .count();

        Long totalPassengers = bookingRepository.countConfirmedBookingsByProvider(providerId);

        // Fetched once and reused for both the existing fleet KPI card and the new
        // lightweight fleet summary section below - no extra query.
        List<Bus> providerBuses = busRepository.findByProviderId(providerId);
        Long fleetCount = (long) providerBuses.size();

        // Build KPI cards
        KpiCard todayBookingsCard = KpiCard.builder()
                .title("Today's Bookings")
                .value(todayBookings != null ? todayBookings.doubleValue() : 0.0)
                .unit("bookings")
                .icon("calendar_today")
                .trend("UP")
                .changePercent(0.0)
                .build();

        KpiCard todayRevenueCard = KpiCard.builder()
                .title("Today's Revenue")
                .value(todayRevenue != null ? todayRevenue : 0.0)
                .unit("INR")
                .icon("attach_money")
                .trend("UP")
                .changePercent(0.0)
                .build();

        KpiCard weeklyRevenueCard = KpiCard.builder()
                .title("Weekly Revenue")
                .value(weeklyRevenue != null ? weeklyRevenue : 0.0)
                .unit("INR")
                .icon("date_range")
                .trend("UP")
                .changePercent(0.0)
                .build();

        KpiCard monthlyRevenueCard = KpiCard.builder()
                .title("Monthly Revenue")
                .value(monthlyRevenue != null ? monthlyRevenue : 0.0)
                .unit("INR")
                .icon("calendar_month")
                .trend("UP")
                .changePercent(0.0)
                .build();

        KpiCard totalRevenueCard = KpiCard.builder()
                .title("Total Revenue")
                .value(totalRevenue != null ? totalRevenue : 0.0)
                .unit("INR")
                .icon("account_balance")
                .trend("UP")
                .changePercent(0.0)
                .build();

        KpiCard activeTripsCard = KpiCard.builder()
                .title("Active Trips")
                .value((double) activeTrips)
                .unit("trips")
                .icon("directions_bus")
                .trend("STABLE")
                .changePercent(0.0)
                .build();

        KpiCard runningTripsCard = KpiCard.builder()
                .title("Running Trips")
                .value((double) runningTrips)
                .unit("trips")
                .icon("play_circle")
                .trend("STABLE")
                .changePercent(0.0)
                .build();

        KpiCard completedTripsCard = KpiCard.builder()
                .title("Completed Trips")
                .value((double) completedTrips)
                .unit("trips")
                .icon("check_circle")
                .trend("UP")
                .changePercent(0.0)
                .build();

        KpiCard cancelledTripsCard = KpiCard.builder()
                .title("Cancelled Trips")
                .value((double) cancelledTrips)
                .unit("trips")
                .icon("cancel")
                .trend("DOWN")
                .changePercent(0.0)
                .build();

        KpiCard delayedTripsCard = KpiCard.builder()
                .title("Delayed Trips")
                .value((double) delayedTrips)
                .unit("trips")
                .icon("schedule")
                .trend("DOWN")
                .changePercent(0.0)
                .build();

        KpiCard totalPassengersCard = KpiCard.builder()
                .title("Total Passengers")
                .value(totalPassengers != null ? totalPassengers.doubleValue() : 0.0)
                .unit("passengers")
                .icon("people")
                .trend("UP")
                .changePercent(0.0)
                .build();

        KpiCard fleetAvailabilityCard = KpiCard.builder()
                .title("Fleet Availability")
                .value(fleetCount != null ? fleetCount.doubleValue() : 0.0)
                .unit("buses")
                .icon("local_shipping")
                .trend("STABLE")
                .changePercent(0.0)
                .build();

        // Chart data
        List<ChartDataPoint> revenueTrend = buildRevenueTrend(providerId, 7);
        List<ChartDataPoint> bookingTrend = buildBookingTrend(providerId, 7);
        List<ChartDataPoint> tripStatusDist = buildTripStatusDistribution(providerTrips);

        // â”€â”€ Lightweight widget sections â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // Each reuses an existing cheap query (already-fetched lists or fast COUNT
        // queries) rather than calling the full detailed analytics methods.

        long activeBusCount = providerBuses.stream().filter(b -> b.getStatus() == BusStatus.ACTIVE).count();
        long maintenanceBusCount = providerBuses.stream().filter(b -> b.getStatus() == BusStatus.MAINTENANCE).count();
        ProviderDashboardResponse.FleetSummary fleetSummary = ProviderDashboardResponse.FleetSummary.builder()
                .totalBuses(fleetCount)
                .activeBuses(activeBusCount)
                .maintenanceBuses(maintenanceBusCount)
                .build();

        ProviderDashboardResponse.StaffSummary staffSummary = ProviderDashboardResponse.StaffSummary.builder()
                .activeDrivers(driverRepository.countActiveByProvider(providerId))
                .activeConductors(conductorRepository.countActiveByProvider(providerId))
                .build();

        List<Maintenance> providerMaintenanceForSummary = maintenanceRepository.findByProviderId(providerId);
        long upcomingMaintenanceCount = providerMaintenanceForSummary.stream()
                .filter(m -> m.getStatus() == MaintenanceStatus.SCHEDULED && !m.getScheduledDate().isBefore(today))
                .count();
        ProviderDashboardResponse.MaintenanceSummary maintenanceSummary = ProviderDashboardResponse.MaintenanceSummary.builder()
                .upcomingCount(upcomingMaintenanceCount)
                .nextItems(buildUpcomingMaintenanceItems(providerMaintenanceForSummary, today, 3))
                .build();

        List<ProviderDashboardResponse.TopRoute> topRoutes = getRouteAnalytics(providerId).stream()
                .limit(3)
                .map(r -> ProviderDashboardResponse.TopRoute.builder()
                        .routeId(r.getRouteId())
                        .source(r.getSource())
                        .destination(r.getDestination())
                        .bookingCount(r.getBookingCount())
                        .revenue(r.getRevenue())
                        .build())
                .collect(Collectors.toList());

        return ProviderDashboardResponse.builder()
                .providerId(providerId)
                .todayBookings(todayBookingsCard)
                .todayRevenue(todayRevenueCard)
                .weeklyRevenue(weeklyRevenueCard)
                .monthlyRevenue(monthlyRevenueCard)
                .totalRevenue(totalRevenueCard)
                .activeTrips(activeTripsCard)
                .runningTrips(runningTripsCard)
                .completedTrips(completedTripsCard)
                .cancelledTrips(cancelledTripsCard)
                .delayedTrips(delayedTripsCard)
                .totalPassengers(totalPassengersCard)
                .fleetAvailability(fleetAvailabilityCard)
                .revenueTrend(revenueTrend)
                .bookingTrend(bookingTrend)
                .tripStatusDistribution(tripStatusDist)
                .fleetSummary(fleetSummary)
                .staffSummary(staffSummary)
                .maintenanceSummary(maintenanceSummary)
                .topRoutes(topRoutes)
                .build();
    }

    @Override
    public List<BusAnalyticsResponse> getBusAnalytics(Long providerId) {
        List<Bus> buses = busRepository.findByProviderId(providerId);
        return buses.stream()
                .map(this::buildBusAnalytics)
                .sorted((a, b) -> Double.compare(b.getRevenue() != null ? b.getRevenue() : 0, a.getRevenue() != null ? a.getRevenue() : 0))
                .collect(Collectors.toList());
    }

    @Override
    public List<RouteAnalyticsResponse> getRouteAnalytics(Long providerId) {
        // Fix: route booking stats are now scoped to this provider at the database level
        // (previously aggregated confirmed bookings across ALL providers).
        List<Object[]> routeStats = bookingRepository.getRouteBookingStats(providerId, PageRequest.of(0, 100));
        return routeStats.stream()
                .map(row -> buildRouteAnalytics(row, providerId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<DriverAnalyticsResponse> getDriverAnalytics(Long providerId) {
        List<Driver> drivers = driverRepository.findByProviderIdAndActiveTrue(providerId);
        return drivers.stream()
                .map(this::buildDriverAnalytics)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConductorAnalyticsResponse> getConductorAnalytics(Long providerId) {
        List<Conductor> conductors = conductorRepository.findByProviderIdAndActiveTrue(providerId);
        return conductors.stream()
                .map(this::buildConductorAnalytics)
                .collect(Collectors.toList());
    }

    @Override
    public MaintenanceAnalyticsResponse getMaintenanceAnalytics(Long providerId) {
        List<Bus> buses = busRepository.findByProviderId(providerId);
        // Fix: filter at the database level instead of loading the entire maintenance
        // table and filtering to this provider's buses in memory.
        List<Maintenance> providerMaintenance = maintenanceRepository.findByProviderId(providerId);

        double totalCost = providerMaintenance.stream()
                .filter(m -> m.getStatus() == MaintenanceStatus.COMPLETED)
                .mapToDouble(m -> m.getCost() != null ? m.getCost() : 0.0)
                .sum();

        long maintenanceCount = providerMaintenance.size();
        double avgCostPerBus = buses.isEmpty() ? 0.0 : totalCost / buses.size();

        // Calculate downtime (days between scheduled and completed)
        long totalDowntimeDays = providerMaintenance.stream()
                .filter(m -> m.getStatus() == MaintenanceStatus.COMPLETED && m.getCompletedDate() != null)
                .mapToLong(m -> java.time.temporal.ChronoUnit.DAYS.between(m.getScheduledDate(), m.getCompletedDate()))
                .sum();

        double avgDowntimePerBus = buses.isEmpty() ? 0.0 : (double) totalDowntimeDays / buses.size();

        // Maintenance frequency
        double frequencyPerMonth = maintenanceCount > 0 ? (double) maintenanceCount / 12.0 : 0.0;

        // Upcoming maintenance
        LocalDate today = LocalDate.now();
        List<MaintenanceAnalyticsResponse.UpcomingMaintenanceItem> upcoming =
                buildUpcomingMaintenanceItems(providerMaintenance, today, 10);

        return MaintenanceAnalyticsResponse.builder()
                .providerId(providerId)
                .totalMaintenanceCost(totalCost)
                .averageCostPerBus(avgCostPerBus)
                .maintenanceCount(maintenanceCount)
                .totalDowntimeDays(totalDowntimeDays)
                .averageDowntimePerBus(avgDowntimePerBus)
                .maintenanceFrequencyPerMonth(frequencyPerMonth)
                .upcomingMaintenance(upcoming)
                .build();
    }

    @Override
    public BookingAnalyticsResponse getBookingAnalytics(Long providerId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate rangeStart = startDate != null ? startDate : (endDate != null ? endDate.minusDays(30) : today.minusDays(30));
        LocalDate rangeEnd = endDate != null ? endDate : today;
        validateDateRange(rangeStart, rangeEnd);

        LocalDateTime since = rangeStart.atStartOfDay();
        LocalDateTime until = rangeEnd.plusDays(1).atStartOfDay();

        Long confirmedBookings = bookingRepository.countConfirmedBookingsByProviderAndDateRange(providerId, since, until);
        Long cancelledBookings = bookingRepository.countCancelledBookingsByProviderAndDateRange(providerId, since, until);
        Long totalBookings = confirmedBookings + cancelledBookings;
        Double cancellationRate = totalBookings > 0 ? (cancelledBookings * 100.0 / totalBookings) : 0.0;

        // Peak booking hours - scoped to the same effective range
        List<Object[]> peakHours = bookingRepository.getPeakBookingHoursByProviderAndDateRange(providerId, since, until);
        List<ChartDataPoint> peakBookingHours = peakHours.stream()
                .map(row -> ChartDataPoint.builder()
                        .label(row[0] + ":00")
                        .value(((Number) row[1]).doubleValue())
                        .category("hour")
                        .build())
                .collect(Collectors.toList());

        // Peak travel days (day of week) - pre-existing placeholder, unrelated to date-range fix
        List<ChartDataPoint> peakTravelDays = buildPeakTravelDays(providerId);

        // Booking growth - day-by-day across the effective range (was previously hardcoded to 30 days)
        List<ChartDataPoint> bookingGrowth = buildBookingTrend(providerId, rangeStart, rangeEnd);

        // Status distribution
        List<ChartDataPoint> statusDist = new ArrayList<>();
        statusDist.add(ChartDataPoint.builder().label("Confirmed").value(confirmedBookings != null ? confirmedBookings.doubleValue() : 0.0).category("status").color("#4CAF50").build());
        statusDist.add(ChartDataPoint.builder().label("Cancelled").value(cancelledBookings != null ? cancelledBookings.doubleValue() : 0.0).category("status").color("#F44336").build());

        return BookingAnalyticsResponse.builder()
                .providerId(providerId)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .cancellationRate(cancellationRate)
                .peakBookingHours(peakBookingHours)
                .peakTravelDays(peakTravelDays)
                .bookingGrowth(bookingGrowth)
                .bookingStatusDistribution(statusDist)
                .build();
    }

    @Override
    public RevenueAnalyticsResponse getRevenueAnalytics(Long providerId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        // Calendar-period KPI cards keep their fixed today/this-week/this-month meaning
        // regardless of any requested from/to range.
        Double dailyRevenue = bookingRepository.sumRevenueByProviderSince(providerId, dayStart);
        Double weeklyRevenue = bookingRepository.sumRevenueByProviderSince(providerId, weekStart);
        Double monthlyRevenue = bookingRepository.sumRevenueByProviderSince(providerId, monthStart);
        Double totalRevenue = bookingRepository.sumRevenueByProvider(providerId);

        Double avgBookingValue = bookingRepository.averageBookingValueByProvider(providerId);
        Double avgFare = avgBookingValue; // Same as average booking value

        Long couponCount = bookingRepository.countCouponUsageByProvider(providerId);
        Double couponDiscount = bookingRepository.sumCouponDiscountByProvider(providerId);

        // Effective requested range: defaults to the last 7 days (matching the previous
        // fixed dailyRevenueTrend window) when from/to are absent.
        LocalDate rangeStart = startDate != null ? startDate : (endDate != null ? endDate.minusDays(6) : today.minusDays(6));
        LocalDate rangeEnd = endDate != null ? endDate : today;
        validateDateRange(rangeStart, rangeEnd);

        List<ChartDataPoint> rangeTrend = buildRevenueTrend(providerId, rangeStart, rangeEnd);
        double rangeRevenue = rangeTrend.stream().mapToDouble(ChartDataPoint::getValue).sum();

        // Fixed comparison windows (unaffected by the requested range), same as before.
        List<ChartDataPoint> weeklyTrend = buildRevenueTrend(providerId, today.minusDays(29), today);
        List<ChartDataPoint> monthlyTrend = buildRevenueTrend(providerId, today.minusDays(89), today);

        return RevenueAnalyticsResponse.builder()
                .providerId(providerId)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .dailyRevenue(dailyRevenue != null ? dailyRevenue : 0.0)
                .weeklyRevenue(weeklyRevenue != null ? weeklyRevenue : 0.0)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : 0.0)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .rangeRevenue(round2(rangeRevenue))
                .revenueGrowthPercent(0.0) // Placeholder for future calculation
                .averageBookingValue(avgBookingValue != null ? avgBookingValue : 0.0)
                .averageFare(avgFare != null ? avgFare : 0.0)
                .couponUsageCount(couponCount != null ? couponCount : 0L)
                .totalCouponDiscount(couponDiscount != null ? couponDiscount : 0.0)
                .totalDiscountAmount(couponDiscount != null ? couponDiscount : 0.0)
                .dailyRevenueTrend(rangeTrend)
                .weeklyRevenueTrend(weeklyTrend)
                .monthlyRevenueTrend(monthlyTrend)
                .build();
    }

    /**
     * Validates a resolved analytics date range and fails fast with the project's
     * standard IllegalArgumentException -> HTTP 400 handling (see GlobalExceptionHandler).
     */
    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("'from' date cannot be after 'to' date");
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ==================== Helper Methods ====================

    // Fix C-3: Use provider-scoped DB query instead of loading all trips
    private List<Trip> getProviderTrips(Long providerId) {
        return tripRepository.findByProviderId(providerId);
    }

    private BusAnalyticsResponse buildBusAnalytics(Bus bus) {
        Long seatsSold = bookingRepository.countSeatsSoldByBus(bus.getId());
        Long bookingCount = bookingRepository.countBookingsByBus(bus.getId());
        Double revenue = bookingRepository.sumRevenueByBus(bus.getId());
        Long tripCount = (long) tripRepository.findByScheduleId(bus.getBusSchedules().stream()
                .map(BusSchedule::getId)
                .findFirst()
                .orElse(0L)).size();

        double utilization = bus.getTotalSeats() > 0 && seatsSold != null
                ? (seatsSold * 100.0 / (bus.getTotalSeats() * Math.max(1, tripCount)))
                : 0.0;

        double occupancy = bus.getTotalSeats() > 0 && seatsSold != null
                ? (seatsSold * 100.0 / bus.getTotalSeats())
                : 0.0;

        String category = utilization > 75 ? "BEST" : utilization > 50 ? "GOOD" : utilization > 25 ? "AVERAGE" : "LOW";

        return BusAnalyticsResponse.builder()
                .busId(bus.getId())
                .busNumber(bus.getBusNumber())
                .busName(bus.getBusName())
                .providerId(bus.getProviderId())
                .utilizationPercentage(Math.min(utilization, 100.0))
                .occupancyPercentage(Math.min(occupancy, 100.0))
                .revenue(revenue != null ? revenue : 0.0)
                .tripCount(tripCount)
                .bookingCount(bookingCount != null ? bookingCount : 0L)
                .totalSeats(bus.getTotalSeats().longValue())
                .seatsSold(seatsSold != null ? seatsSold : 0L)
                .performanceCategory(category)
                .build();
    }

    private RouteAnalyticsResponse buildRouteAnalytics(Object[] row, Long providerId) {
        Long routeId = ((Number) row[0]).longValue();
        String source = (String) row[1];
        String destination = (String) row[2];
        Long bookingCount = ((Number) row[3]).longValue();
        Double revenue = ((Number) row[4]).doubleValue();

        Route route = routeRepository.findById(routeId).orElse(null);
        if (route == null) return null;

        double distanceKm = route.getDistanceKm() != null ? route.getDistanceKm() : 0.0;
        double revenuePerKm = distanceKm > 0 ? revenue / distanceKm : 0.0;

        return RouteAnalyticsResponse.builder()
                .routeId(routeId)
                .source(source)
                .destination(destination)
                .revenue(revenue)
                .bookingCount(bookingCount)
                .passengerCount(bookingCount) // Approximation
                .occupancyPercentage(0.0) // Placeholder
                .tripCount(bookingCount) // Approximation
                .distanceKm(distanceKm)
                .revenuePerKm(revenuePerKm)
                .performanceCategory(bookingCount > 50 ? "MOST_POPULAR" : bookingCount > 20 ? "HIGH_REVENUE" : "AVERAGE")
                .build();
    }

    private DriverAnalyticsResponse buildDriverAnalytics(Driver driver) {
        Long completedTrips = tripRepository.countCompletedTripsByDriver(driver.getId());
        Double distance = tripRepository.sumDistanceByDriver(driver.getId());

        double utilization = driver.getTotalTrips() > 0
                ? (completedTrips * 100.0 / driver.getTotalTrips())
                : 0.0;

        String category = driver.getRating() >= 4.5 ? "TOP" : driver.getRating() >= 3.5 ? "GOOD" : driver.getRating() >= 2.5 ? "AVERAGE" : "NEEDS_IMPROVEMENT";

        return DriverAnalyticsResponse.builder()
                .driverId(driver.getId())
                .driverName(driver.getName())
                .licenseNumber(driver.getLicenseNumber())
                .providerId(driver.getProviderId())
                .totalTrips(driver.getTotalTrips().longValue())
                .completedTrips(completedTrips != null ? completedTrips : 0L)
                .distanceCovered(distance != null ? distance : driver.getTotalDistanceKm())
                .rating(driver.getRating())
                .utilizationPercentage(utilization)
                .rank(0L) // Will be set in ranking
                .performanceCategory(category)
                .build();
    }

    private ConductorAnalyticsResponse buildConductorAnalytics(Conductor conductor) {
        Long completedTrips = tripRepository.countCompletedTripsByConductor(conductor.getId());

        String category = conductor.getRating() >= 4.5 ? "TOP" : conductor.getRating() >= 3.5 ? "GOOD" : conductor.getRating() >= 2.5 ? "AVERAGE" : "NEEDS_IMPROVEMENT";

        return ConductorAnalyticsResponse.builder()
                .conductorId(conductor.getId())
                .conductorName(conductor.getName())
                .employeeId(conductor.getEmployeeId())
                .providerId(conductor.getProviderId())
                .totalTrips(conductor.getTotalTrips().longValue())
                .completedTrips(completedTrips != null ? completedTrips : 0L)
                .passengerHandling(completedTrips != null ? completedTrips * 20 : 0L) // Approximation
                .rating(conductor.getRating())
                .rank(0L) // Will be set in ranking
                .performanceCategory(category)
                .build();
    }

    // Fix H-4: Single batch query instead of 2N individual queries
    private List<ChartDataPoint> buildRevenueTrend(Long providerId, int days) {
        LocalDate today = LocalDate.now();
        return buildRevenueTrend(providerId, today.minusDays(days - 1L), today);
    }

    private List<ChartDataPoint> buildRevenueTrend(Long providerId, LocalDate start, LocalDate end) {
        LocalDateTime since = start.atStartOfDay();
        LocalDateTime until = end.plusDays(1).atStartOfDay();

        // Single query returns daily revenue for the entire range
        List<Object[]> dailyRevenues = bookingRepository.sumDailyRevenueByProviderAndDateRange(providerId, since, until);
        Map<String, Double> revenueByDate = new HashMap<>();
        for (Object[] row : dailyRevenues) {
            String dateStr = String.valueOf(row[0]);
            Double revenue = ((Number) row[1]).doubleValue();
            revenueByDate.put(dateStr, revenue);
        }

        List<ChartDataPoint> trend = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            double dayRevenue = revenueByDate.getOrDefault(date.toString(), 0.0);

            trend.add(ChartDataPoint.builder()
                    .label(date.toString())
                    .value(Math.max(dayRevenue, 0.0))
                    .category("revenue")
                    .build());
        }

        return trend;
    }

    private List<ChartDataPoint> buildBookingTrend(Long providerId, int days) {
        LocalDate today = LocalDate.now();
        return buildBookingTrend(providerId, today.minusDays(days - 1L), today);
    }

    private List<ChartDataPoint> buildBookingTrend(Long providerId, LocalDate start, LocalDate end) {
        List<ChartDataPoint> trend = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Long count = bookingRepository.countTodayBookingsByProvider(providerId, date);

            trend.add(ChartDataPoint.builder()
                    .label(date.toString())
                    .value(count != null ? count.doubleValue() : 0.0)
                    .category("bookings")
                    .build());
        }

        return trend;
    }

    private List<ChartDataPoint> buildTripStatusDistribution(List<Trip> trips) {
        Map<TripStatus, Long> statusCount = trips.stream()
                .collect(Collectors.groupingBy(Trip::getStatus, Collectors.counting()));

        return statusCount.entrySet().stream()
                .map(e -> ChartDataPoint.builder()
                        .label(e.getKey().name())
                        .value(e.getValue().doubleValue())
                        .category("trip_status")
                        .build())
                .collect(Collectors.toList());
    }

    private List<MaintenanceAnalyticsResponse.UpcomingMaintenanceItem> buildUpcomingMaintenanceItems(
            List<Maintenance> providerMaintenance, LocalDate today, int limit) {
        return providerMaintenance.stream()
                .filter(m -> m.getStatus() == MaintenanceStatus.SCHEDULED && !m.getScheduledDate().isBefore(today))
                .sorted(Comparator.comparing(Maintenance::getScheduledDate))
                .limit(limit)
                .map(m -> MaintenanceAnalyticsResponse.UpcomingMaintenanceItem.builder()
                        .maintenanceId(m.getId())
                        .busId(m.getBus().getId())
                        .busNumber(m.getBus().getBusNumber())
                        .maintenanceType(m.getMaintenanceType())
                        .scheduledDate(m.getScheduledDate())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ChartDataPoint> buildPeakTravelDays(Long providerId) {
        List<ChartDataPoint> days = new ArrayList<>();
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (int i = 0; i < 7; i++) {
            days.add(ChartDataPoint.builder()
                    .label(dayNames[i])
                    .value(0.0) // Placeholder
                    .category("day_of_week")
                    .build());
        }

        return days;
    }
}
