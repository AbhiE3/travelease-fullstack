package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.ReportExportRequest;
import com.travelease.backend.busbooking.dto.request.ReportFilterRequest;
import com.travelease.backend.busbooking.dto.response.ApiResponse;
import com.travelease.backend.busbooking.dto.response.ReportHistoryResponse;
import com.travelease.backend.busbooking.dto.response.ReportResponse;
import com.travelease.backend.busbooking.entity.enums.ReportType;
import com.travelease.backend.busbooking.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Enterprise Reporting & Export", description = "Report generation and export APIs")
public class ReportController {

    private final ReportService reportService;

    // ==================== REPORT GENERATION ====================

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate report by type with filters")
    public ResponseEntity<ApiResponse<ReportResponse>> generateReport(
            @RequestParam ReportType reportType,
            @RequestBody(required = false) ReportFilterRequest filters) {

        if (filters == null) filters = new ReportFilterRequest();

        ReportResponse response = reportService.generateReport(reportType, filters);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                response.getReportName() + " generated successfully", response, "/api/reports/generate"));
    }

    // ==================== EXPORT ====================

    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export report as CSV or Excel")
    public ResponseEntity<?> exportReport(@Valid @RequestBody ReportExportRequest request) {
        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(request.getProviderId());
        filters.setBusId(request.getBusId());
        filters.setRouteId(request.getRouteId());
        filters.setDriverId(request.getDriverId());
        filters.setConductorId(request.getConductorId());
        filters.setStartDate(request.getFrom());
        filters.setEndDate(request.getTo());

        if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
            byte[] excel = reportService.exportReportToExcel(request.getReportType(), filters);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(request.getReportType().name().toLowerCase() + "_report.xlsx")
                    .build());
            return ResponseEntity.ok().headers(headers).body(excel);
        } else {
            String csv = reportService.exportReportToCsv(request.getReportType(), filters);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(request.getReportType().name().toLowerCase() + "_report.csv")
                    .build());
            return ResponseEntity.ok().headers(headers).body(csv);
        }
    }

    // ==================== REPORT HISTORY ====================

    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get report history with filters")
    public ResponseEntity<ApiResponse<List<ReportHistoryResponse>>> getReportHistory(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "generatedAt") Pageable pageable) {
        List<ReportHistoryResponse> response = reportService.getReportHistory(providerId, reportType, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Report history fetched successfully", response, "/api/reports/history"));
    }
}
