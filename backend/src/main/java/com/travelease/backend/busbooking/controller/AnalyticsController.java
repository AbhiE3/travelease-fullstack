package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.response.*;
import com.travelease.backend.busbooking.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Dashboard", description = "Transport provider analytics and dashboard APIs")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ==================== PROVIDER DASHBOARD ====================

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get provider dashboard summary")
    public ResponseEntity<ApiResponse<ProviderDashboardResponse>> getProviderDashboard(
            @RequestParam Long providerId) {
        ProviderDashboardResponse response = analyticsService.getProviderDashboard(providerId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Dashboard data fetched successfully", response, "/api/analytics/dashboard"));
    }

    // ==================== BUS ANALYTICS ====================

    @GetMapping("/buses")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get bus analytics for provider")
    public ResponseEntity<ApiResponse<List<BusAnalyticsResponse>>> getBusAnalytics(
            @RequestParam Long providerId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer size) {
        List<BusAnalyticsResponse> response = analyticsService.getBusAnalytics(providerId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Bus analytics fetched successfully", response, "/api/analytics/buses"));
    }

    // ==================== ROUTE ANALYTICS ====================

    @GetMapping("/routes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get route analytics for provider")
    public ResponseEntity<ApiResponse<List<RouteAnalyticsResponse>>> getRouteAnalytics(
            @RequestParam Long providerId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer size) {
        List<RouteAnalyticsResponse> response = analyticsService.getRouteAnalytics(providerId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Route analytics fetched successfully", response, "/api/analytics/routes"));
    }

    // ==================== DRIVER ANALYTICS ====================

    @GetMapping("/drivers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get driver analytics for provider")
    public ResponseEntity<ApiResponse<List<DriverAnalyticsResponse>>> getDriverAnalytics(
            @RequestParam Long providerId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer size) {
        List<DriverAnalyticsResponse> response = analyticsService.getDriverAnalytics(providerId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Driver analytics fetched successfully", response, "/api/analytics/drivers"));
    }

    // ==================== CONDUCTOR ANALYTICS ====================

    @GetMapping("/conductors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get conductor analytics for provider")
    public ResponseEntity<ApiResponse<List<ConductorAnalyticsResponse>>> getConductorAnalytics(
            @RequestParam Long providerId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer size) {
        List<ConductorAnalyticsResponse> response = analyticsService.getConductorAnalytics(providerId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conductor analytics fetched successfully", response, "/api/analytics/conductors"));
    }

    // ==================== MAINTENANCE ANALYTICS ====================

    @GetMapping("/maintenance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get maintenance analytics for provider")
    public ResponseEntity<ApiResponse<MaintenanceAnalyticsResponse>> getMaintenanceAnalytics(
            @RequestParam Long providerId) {
        MaintenanceAnalyticsResponse response = analyticsService.getMaintenanceAnalytics(providerId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Maintenance analytics fetched successfully", response, "/api/analytics/maintenance"));
    }

    // ==================== BOOKING ANALYTICS ====================

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get booking analytics for provider")
    public ResponseEntity<ApiResponse<BookingAnalyticsResponse>> getBookingAnalytics(
            @RequestParam Long providerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        // Default resolution and from>to validation now live in AnalyticsServiceImpl,
        // so it applies consistently regardless of caller.
        BookingAnalyticsResponse response = analyticsService.getBookingAnalytics(providerId, from, to);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Booking analytics fetched successfully", response, "/api/analytics/bookings"));
    }

    // ==================== REVENUE ANALYTICS ====================

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get revenue analytics for provider")
    public ResponseEntity<ApiResponse<RevenueAnalyticsResponse>> getRevenueAnalytics(
            @RequestParam Long providerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        // Default resolution and from>to validation now live in AnalyticsServiceImpl,
        // so it applies consistently regardless of caller.
        RevenueAnalyticsResponse response = analyticsService.getRevenueAnalytics(providerId, from, to);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Revenue analytics fetched successfully", response, "/api/analytics/revenue"));
    }
}
