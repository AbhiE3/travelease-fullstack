package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.BusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusSearchCriteriaRequest {

    // Required fields
    @NotBlank(message = "Source is required")
    private String source;

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotNull(message = "Travel date is required")
    private LocalDate travelDate;

    // Optional filters
    private BusType busType;
    private Boolean ac; // true = AC, false = Non-AC
    private String category; // SLEEPER, SEMI_SLEEPER, SEATER, LUXURY
    private List<String> amenities;
    private Integer minAvailableSeats;
    private LocalTime departureTimeFrom;
    private LocalTime departureTimeTo;
    private LocalTime arrivalTimeFrom;
    private LocalTime arrivalTimeTo;

    @Positive(message = "Minimum fare must be positive")
    private Double minFare;

    @Positive(message = "Maximum fare must be positive")
    private Double maxFare;

    // Sorting: LOWEST_FARE, HIGHEST_FARE, EARLIEST_DEPARTURE, LATEST_DEPARTURE, FASTEST_JOURNEY
    @Builder.Default
    private String sortBy = "EARLIEST_DEPARTURE";

    // Pagination
    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;
}
