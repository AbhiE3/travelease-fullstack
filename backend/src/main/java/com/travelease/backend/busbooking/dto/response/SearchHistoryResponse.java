package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single search history entry.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistoryResponse {

    private Long id;
    private String source;
    private String destination;
    private LocalDate travelDate;
    private LocalDateTime searchedAt;
}
