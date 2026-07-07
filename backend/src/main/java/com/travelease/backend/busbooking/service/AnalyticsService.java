package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.response.*;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsService {

    // Provider Dashboard
    ProviderDashboardResponse getProviderDashboard(Long providerId);

    // Bus Analytics
    List<BusAnalyticsResponse> getBusAnalytics(Long providerId);

    // Route Analytics
    List<RouteAnalyticsResponse> getRouteAnalytics(Long providerId);

    // Driver Analytics
    List<DriverAnalyticsResponse> getDriverAnalytics(Long providerId);

    // Conductor Analytics
    List<ConductorAnalyticsResponse> getConductorAnalytics(Long providerId);

    // Maintenance Analytics
    MaintenanceAnalyticsResponse getMaintenanceAnalytics(Long providerId);

    // Booking Analytics
    BookingAnalyticsResponse getBookingAnalytics(Long providerId, LocalDate startDate, LocalDate endDate);

    // Revenue Analytics
    RevenueAnalyticsResponse getRevenueAnalytics(Long providerId, LocalDate startDate, LocalDate endDate);
}
