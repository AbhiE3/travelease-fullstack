package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.ReportExportRequest;
import com.travelease.backend.busbooking.dto.request.ReportFilterRequest;
import com.travelease.backend.busbooking.dto.response.ApiResponse;
import com.travelease.backend.busbooking.dto.response.ReportHistoryResponse;
import com.travelease.backend.busbooking.dto.response.ReportResponse;
import com.travelease.backend.busbooking.entity.enums.ReportType;
import com.travelease.backend.busbooking.security.SecurityUtil;
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
    private final SecurityUtil securityUtil;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Generate report by type with filters", description = "Generate report by type with filters")
    public ResponseEntity<ApiResponse<ReportResponse>> generateReport(
            @RequestParam ReportType reportType,
            @RequestBody(required = false) ReportFilterRequest filters) {

        if (filters == null) filters = new ReportFilterRequest();
        filters.setProviderId(securityUtil.resolveEffectiveProviderId(filters.getProviderId()));

        ReportResponse response = reportService.generateReport(reportType, filters);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                response.getReportName() + " generated successfully", response, "/api/reports/generate"));
    }

    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Export report as CSV or Excel", description = "Export report as CSV or Excel")
    public ResponseEntity<?> exportReport(@Valid @RequestBody ReportExportRequest request) {
        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(securityUtil.resolveEffectiveProviderId(request.getProviderId()));
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

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get report history with filters", description = "Get report history with filters")
    public ResponseEntity<ApiResponse<List<ReportHistoryResponse>>> getReportHistory(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "generatedAt") Pageable pageable) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        List<ReportHistoryResponse> response = reportService.getReportHistory(effectiveProviderId, reportType, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Report history fetched successfully", response, "/api/reports/history"));
    }
}

