package com.travelease.backend.busbooking.scheduler;

import com.travelease.backend.busbooking.dto.request.ReportFilterRequest;
import com.travelease.backend.busbooking.entity.enums.ReportType;
import com.travelease.backend.busbooking.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Simulated report scheduler.
 * Generates reports on a schedule (simulation only - no email or external storage).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportScheduler {

    private final ReportService reportService;

    // Fix H-7: Configurable default provider ID for scheduled reports
    @Value("${app.scheduler.default-provider-id:1}")
    private Long defaultProviderId;

    /**
     * Daily report generation - runs every day at 11:00 PM.
     * Generates booking and revenue reports for the current day.
     */
    @Scheduled(cron = "0 0 23 * * *")
    public void generateDailyReports() {
        log.info("Starting daily report generation...");

        LocalDate today = LocalDate.now();

        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(defaultProviderId);
        filters.setStartDate(today);
        filters.setEndDate(today);

        try {
            reportService.generateBookingReport(filters);
            log.info("Daily booking report generated successfully");

            reportService.generateRevenueReport(filters);
            log.info("Daily revenue report generated successfully");
        } catch (Exception e) {
            log.error("Error generating daily reports: {}", e.getMessage());
        }
    }

    /**
     * Weekly report generation - runs every Monday at 11:30 PM.
     * Generates comprehensive reports for the past week.
     */
    @Scheduled(cron = "0 30 23 * * MON")
    public void generateWeeklyReports() {
        log.info("Starting weekly report generation...");

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);

        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(defaultProviderId);
        filters.setStartDate(weekStart);
        filters.setEndDate(today);

        try {
            reportService.generateBookingReport(filters);
            reportService.generateRevenueReport(filters);
            reportService.generateBusPerformanceReport(filters);
            reportService.generateRoutePerformanceReport(filters);
            log.info("Weekly reports generated successfully");
        } catch (Exception e) {
            log.error("Error generating weekly reports: {}", e.getMessage());
        }
    }

    /**
     * Monthly report generation - runs on the 1st of every month at 11:45 PM.
     * Generates comprehensive reports for the past month.
     */
    @Scheduled(cron = "0 45 23 1 * *")
    public void generateMonthlyReports() {
        log.info("Starting monthly report generation...");

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.minusMonths(1);

        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(defaultProviderId);
        filters.setStartDate(monthStart);
        filters.setEndDate(today);

        try {
            reportService.generateBookingReport(filters);
            reportService.generateRevenueReport(filters);
            reportService.generatePassengerReport(filters);
            reportService.generateBusPerformanceReport(filters);
            reportService.generateRoutePerformanceReport(filters);
            reportService.generateDriverPerformanceReport(filters);
            reportService.generateConductorPerformanceReport(filters);
            reportService.generateFleetUtilizationReport(filters);
            reportService.generateMaintenanceReport(filters);
            reportService.generateRefundReport(filters);
            reportService.generateCancellationReport(filters);
            log.info("Monthly reports generated successfully");
        } catch (Exception e) {
            log.error("Error generating monthly reports: {}", e.getMessage());
        }
    }
}
