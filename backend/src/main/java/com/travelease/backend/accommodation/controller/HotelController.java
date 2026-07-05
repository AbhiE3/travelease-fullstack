package com.travelease.backend.accommodation.controller;

import com.travelease.backend.accommodation.dto.HotelDetailsResponse;
import com.travelease.backend.accommodation.dto.HotelResponse;
import com.travelease.backend.accommodation.dto.HotelReviewResponse;
import com.travelease.backend.accommodation.dto.ReviewRequest;
import com.travelease.backend.accommodation.service.AccommodationService;
import com.travelease.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final AccommodationService accommodationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HotelResponse>>> searchHotels(
            @RequestParam(required = false) Integer destinationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "q") String query
    ) {
        List<HotelResponse> response = accommodationService.searchHotels(destinationId, status, query);
        return ResponseEntity.ok(ApiResponse.success(response, "Hotels retrieved"));
    }

    @GetMapping("/{hotelId}")
    public ResponseEntity<ApiResponse<HotelDetailsResponse>> getHotel(@PathVariable UUID hotelId) {
        HotelDetailsResponse response = accommodationService.getHotelDetails(hotelId);
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel details retrieved"));
    }

    @GetMapping("/{hotelId}/reviews")
    public ResponseEntity<ApiResponse<List<HotelReviewResponse>>> getReviews(@PathVariable UUID hotelId) {
        List<HotelReviewResponse> response = accommodationService.getReviews(hotelId);
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel reviews retrieved"));
    }

    @PostMapping("/{hotelId}/reviews")
    public ResponseEntity<ApiResponse<HotelReviewResponse>> addReview(
            @PathVariable UUID hotelId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        HotelReviewResponse response = accommodationService.addReview(hotelId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Hotel review added"));
    }

    @PutMapping("/{hotelId}/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<HotelReviewResponse>> updateReview(
            @PathVariable UUID hotelId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        HotelReviewResponse response = accommodationService.updateReview(
                hotelId,
                reviewId,
                request,
                authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel review updated"));
    }

    @DeleteMapping("/{hotelId}/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID hotelId,
            @PathVariable UUID reviewId,
            Authentication authentication
    ) {
        accommodationService.deleteReview(hotelId, reviewId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Hotel review deleted"));
    }
}
