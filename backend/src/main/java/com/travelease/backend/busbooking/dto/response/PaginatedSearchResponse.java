package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paginated response wrapper for search results and other list endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginatedSearchResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
