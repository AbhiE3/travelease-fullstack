package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportExportRequest {

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotNull(message = "Export format is required")
    private String format; // CSV or EXCEL

    private Long providerId;
    private Long busId;
    private Long routeId;
    private Long driverId;
    private Long conductorId;
    private LocalDate from;
    private LocalDate to;
}
