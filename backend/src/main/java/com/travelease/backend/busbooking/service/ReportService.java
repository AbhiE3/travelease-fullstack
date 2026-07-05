package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.ReportFilterRequest;
import com.travelease.backend.busbooking.dto.response.ReportHistoryResponse;
import com.travelease.backend.busbooking.dto.response.ReportResponse;
import com.travelease.backend.busbooking.entity.enums.ReportType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    // Report generation
    ReportResponse generateReport(ReportType reportType, ReportFilterRequest filters);

    // Specific report types
    ReportResponse generateBookingReport(ReportFilterRequest filters);
    ReportResponse generateRevenueReport(ReportFilterRequest filters);
    ReportResponse generatePassengerReport(ReportFilterRequest filters);
    ReportResponse generateBusPerformanceReport(ReportFilterRequest filters);
    ReportResponse generateRoutePerformanceReport(ReportFilterRequest filters);
    ReportResponse generateDriverPerformanceReport(ReportFilterRequest filters);
    ReportResponse generateConductorPerformanceReport(ReportFilterRequest filters);
    ReportResponse generateFleetUtilizationReport(ReportFilterRequest filters);
    ReportResponse generateMaintenanceReport(ReportFilterRequest filters);
    ReportResponse generateRefundReport(ReportFilterRequest filters);
    ReportResponse generateCancellationReport(ReportFilterRequest filters);

    // Export
    byte[] exportReportToExcel(ReportType reportType, ReportFilterRequest filters);
    String exportReportToCsv(ReportType reportType, ReportFilterRequest filters);

    // Report history (consolidated - use Pageable for recent: page=0&size=10&sort=generatedAt,desc)
    List<ReportHistoryResponse> getReportHistory(Long providerId, ReportType reportType, LocalDate from, LocalDate to, Pageable pageable);
}
