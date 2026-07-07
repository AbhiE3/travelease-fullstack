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
    private final com.travelease.backend.busbooking.security.SecurityUtil securityUtil;

    // ==================== PROVIDER DASHBOARD ====================

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get provider dashboard summary")
    public ResponseEntity<ApiResponse<ProviderDashboardResponse>> getProviderDashboard(
            @RequestParam(required = false) Long providerId) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        ProviderDashboardResponse response = analyticsService.getProviderDashboard(effectiveProviderId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Dashboard data fetched successfully", response, "/api/analytics/dashboard"));
    }

    // ==================== BUS ANALYTICS ====================

    @GetMapping("/buses")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get bus analytics for provider")
    public ResponseEntity<ApiResponse<List<BusAnalyticsResponse>>> getBusAnalytics(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer size) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        List<BusAnalyticsResponse> response = analyticsService.getBusAnalytics(effectiveProviderId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Bus analytics fetched successfully", response, "/api/analytics/buses"));
    }

    // ==================== ROUTE ANALYTICS ====================

    @GetMapping("/routes")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get route analytics for provider")
    public ResponseEntity<ApiResponse<List<RouteAnalyticsResponse>>> getRouteAnalytics(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer size) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        List<RouteAnalyticsResponse> response = analyticsService.getRouteAnalytics(effectiveProviderId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Route analytics fetched successfully", response, "/api/analytics/routes"));
    }

    // ==================== DRIVER ANALYTICS ====================

    @GetMapping("/drivers")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get driver analytics for provider")
    public ResponseEntity<ApiResponse<List<DriverAnalyticsResponse>>> getDriverAnalytics(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer size) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        List<DriverAnalyticsResponse> response = analyticsService.getDriverAnalytics(effectiveProviderId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Driver analytics fetched successfully", response, "/api/analytics/drivers"));
    }

    // ==================== CONDUCTOR ANALYTICS ====================

    @GetMapping("/conductors")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get conductor analytics for provider")
    public ResponseEntity<ApiResponse<List<ConductorAnalyticsResponse>>> getConductorAnalytics(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer size) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        List<ConductorAnalyticsResponse> response = analyticsService.getConductorAnalytics(effectiveProviderId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conductor analytics fetched successfully", response, "/api/analytics/conductors"));
    }

    // ==================== MAINTENANCE ANALYTICS ====================

    @GetMapping("/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get maintenance analytics for provider")
    public ResponseEntity<ApiResponse<MaintenanceAnalyticsResponse>> getMaintenanceAnalytics(
            @RequestParam(required = false) Long providerId) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        MaintenanceAnalyticsResponse response = analyticsService.getMaintenanceAnalytics(effectiveProviderId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Maintenance analytics fetched successfully", response, "/api/analytics/maintenance"));
    }

    // ==================== BOOKING ANALYTICS ====================

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get booking analytics for provider")
    public ResponseEntity<ApiResponse<BookingAnalyticsResponse>> getBookingAnalytics(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);

        // Default resolution and from>to validation now live in AnalyticsServiceImpl,
        // so it applies consistently regardless of caller.
        BookingAnalyticsResponse response = analyticsService.getBookingAnalytics(effectiveProviderId, from, to);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Booking analytics fetched successfully", response, "/api/analytics/bookings"));
    }

    // ==================== REVENUE ANALYTICS ====================

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get revenue analytics for provider")
    public ResponseEntity<ApiResponse<RevenueAnalyticsResponse>> getRevenueAnalytics(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);

        // Default resolution and from>to validation now live in AnalyticsServiceImpl,
        // so it applies consistently regardless of caller.
        RevenueAnalyticsResponse response = analyticsService.getRevenueAnalytics(effectiveProviderId, from, to);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Revenue analytics fetched successfully", response, "/api/analytics/revenue"));
    }
}
