package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.BusSearchCriteriaRequest;
import com.travelease.backend.busbooking.dto.request.ScheduleRequest;
import com.travelease.backend.busbooking.dto.response.*;
import com.travelease.backend.busbooking.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedule Management", description = "Endpoints for managing bus schedules and searching buses")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    @Operation(summary = "Get all schedules")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getAllSchedules() {
        List<ScheduleResponse> response = scheduleService.getAllSchedules();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Schedules fetched successfully", response, "/api/schedules"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getScheduleById(@PathVariable Long id) {
        ScheduleResponse response = scheduleService.getScheduleById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Schedule fetched successfully", response, "/api/schedules/" + id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search buses by source, destination, and travel date")
    public ResponseEntity<ApiResponse<List<BusSearchResponse>>> searchBuses(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<BusSearchResponse> response = scheduleService.searchBuses(source, destination, date);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Bus search completed successfully", response, "/api/schedules/search"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse response = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Schedule created successfully", response, "/api/schedules"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update schedule by ID")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(@PathVariable Long id,
                                                                         @Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse response = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Schedule updated successfully", response, "/api/schedules/" + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel/delete schedule by ID")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Schedule cancelled successfully", new MessageResponse("Schedule cancelled successfully"), "/api/schedules/" + id));
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // Phase 4 Ã¢â‚¬â€œ Smart Search & Traveller Discovery
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @PostMapping("/search/advanced")
    @Operation(summary = "Smart bus search with multi-filter, pagination, and dynamic sorting")
    public ResponseEntity<ApiResponse<PaginatedSearchResponse<SmartSearchResponse>>> smartSearch(
            @Valid @RequestBody BusSearchCriteriaRequest criteria) {
        PaginatedSearchResponse<SmartSearchResponse> response = scheduleService.smartSearch(criteria);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Smart search completed successfully", response, "/api/schedules/search/advanced"));
    }

    @GetMapping("/search/popular-routes")
    @Operation(summary = "Get popular routes based on search frequency")
    public ResponseEntity<ApiResponse<List<PopularRouteResponse>>> getPopularRoutes(
            @RequestParam(defaultValue = "10") int limit) {
        List<PopularRouteResponse> response = scheduleService.getPopularRoutes(limit);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Popular routes fetched successfully", response, "/api/schedules/search/popular-routes"));
    }

    @GetMapping("/search/history")
    @Operation(summary = "Get search history for current user (use sort=searchedAt,desc&size=5 for recent)")
    public ResponseEntity<ApiResponse<List<SearchHistoryResponse>>> getSearchHistory(
            @PageableDefault(size = 20, sort = "searchedAt") Pageable pageable) {
        List<SearchHistoryResponse> response = scheduleService.getSearchHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Search history fetched successfully", response, "/api/schedules/search/history"));
    }

    @GetMapping("/search/suggestions")
    @Operation(summary = "Get search suggestions based on previous bookings")
    public ResponseEntity<ApiResponse<List<SearchSuggestionResponse>>> getSearchSuggestions(
            @RequestParam(defaultValue = "5") int limit) {
        List<SearchSuggestionResponse> response = scheduleService.getSearchSuggestions(limit);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Search suggestions fetched successfully", response, "/api/schedules/search/suggestions"));
    }

    @GetMapping("/search/frequently-booked")
    @Operation(summary = "Get frequently booked routes across all users")
    public ResponseEntity<ApiResponse<List<PopularRouteResponse>>> getFrequentlyBookedRoutes(
            @RequestParam(defaultValue = "10") int limit) {
        List<PopularRouteResponse> response = scheduleService.getFrequentlyBookedRoutes(limit);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Frequently booked routes fetched successfully", response, "/api/schedules/search/frequently-booked"));
    }
}
