package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Generic report response with summary, data, and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private String reportName;
    private String reportType;
    private LocalDateTime generatedAt;
    private String generatedBy;

    // Summary statistics
    private ReportSummaryResponse summary;

    // Report data (list of maps for flexibility)
    private List<Map<String, Object>> data;

    // Pagination
    private int page;
    private int size;
    private long totalRecords;
    private int totalPages;

    // Applied filters
    private Map<String, Object> appliedFilters;
}
