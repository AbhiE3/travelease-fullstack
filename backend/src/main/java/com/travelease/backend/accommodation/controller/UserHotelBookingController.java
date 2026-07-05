package com.travelease.backend.accommodation.controller;

import com.travelease.backend.accommodation.dto.HotelBookingResponse;
import com.travelease.backend.accommodation.service.AccommodationService;
import com.travelease.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/hotel-bookings")
@RequiredArgsConstructor
public class UserHotelBookingController {

    private final AccommodationService accommodationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HotelBookingResponse>>> myBookings(Authentication authentication) {
        List<HotelBookingResponse> response = accommodationService.getMyBookings(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel booking history retrieved"));
    }
}
