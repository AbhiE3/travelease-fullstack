package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.response.ApiResponse;
import com.travelease.backend.busbooking.dto.response.BookingResponse;
import com.travelease.backend.busbooking.dto.response.DashboardStatsResponse;
import com.travelease.backend.busbooking.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("busbookingAdminController")
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only endpoints for managing the system")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard statistics", description = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse response = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(200, "Dashboard statistics fetched successfully", response, "/api/admin/dashboard"));
    }

    @GetMapping("/schedules/{scheduleId}/bookings")
    @Operation(summary = "Get all bookings for a specific schedule", description = "Get all bookings for a specific schedule")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getScheduleBookings(@PathVariable Long scheduleId) {
        List<BookingResponse> response = adminService.getScheduleBookings(scheduleId);
        return ResponseEntity.ok(ApiResponse.success(200, "Schedule bookings fetched successfully", response, "/api/admin/schedules/" + scheduleId + "/bookings"));
    }
}

