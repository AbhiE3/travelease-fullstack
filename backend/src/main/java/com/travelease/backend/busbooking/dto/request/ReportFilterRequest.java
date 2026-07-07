package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import com.travelease.backend.busbooking.entity.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Report filter request.
 * Supports all filter dimensions for report generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterRequest {

    // Date range
    private LocalDate startDate;
    private LocalDate endDate;

    // Entity filters
    private Long providerId;
    private Long busId;
    private Long routeId;
    private Long driverId;
    private Long conductorId;

    // Status filters
    private BookingStatus bookingStatus;
    private String tripStatus;
    private String paymentStatus;
    private RefundStatus refundStatus;

    // Pagination
    private int page = 0;
    private int size = 50;
    private String sortBy = "id";
    private String sortDirection = "DESC";
}
