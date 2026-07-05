package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Report history response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportHistoryResponse {

    private Long id;
    private String reportName;
    private String reportType;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private String appliedFilters;
    private String exportFormat;
    private Integer recordCount;
    private Long providerId;
}
