package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.ReportFilterRequest;
import com.travelease.backend.busbooking.dto.response.ReportResponse;
import com.travelease.backend.busbooking.entity.Booking;
import com.travelease.backend.busbooking.entity.Bus;
import com.travelease.backend.busbooking.entity.BusSchedule;
import com.travelease.backend.busbooking.entity.Maintenance;
import com.travelease.backend.busbooking.entity.Refund;
import com.travelease.backend.busbooking.entity.Route;
import com.travelease.backend.busbooking.entity.enums.MaintenanceStatus;
import com.travelease.backend.busbooking.entity.enums.RefundStatus;
import com.travelease.backend.busbooking.entity.enums.ReportType;
import com.travelease.backend.busbooking.repository.MaintenanceRepository;
import com.travelease.backend.busbooking.repository.RefundRepository;
import com.travelease.backend.busbooking.repository.ReportHistoryRepository;
import com.travelease.backend.busbooking.util.ReportExportUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplProviderIsolationTest {

    @Mock
    private RefundRepository refundRepository;
    @Mock
    private MaintenanceRepository maintenanceRepository;
    @Mock
    private ReportHistoryRepository reportHistoryRepository;
    @Mock
    private ReportExportUtil reportExportUtil;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Bus bus(Long id, Long providerId) {
        return Bus.builder().id(id).busNumber("BUS-" + id).providerId(providerId).build();
    }

    private Refund refundFor(Bus bus) {
        Route route = Route.builder().id(1L).source("A").destination("B").build();
        BusSchedule schedule = BusSchedule.builder().id(1L).bus(bus).route(route).travelDate(LocalDate.now()).build();
        Booking booking = Booking.builder().id(1L).schedule(schedule).bookingReference("REF-1").build();
        return Refund.builder()
                .id(1L)
                .booking(booking)
                .refundReference("RFD-1")
                .originalAmount(500.0)
                .netRefundable(450.0)
                .status(RefundStatus.COMPLETED)
                .initiatedAt(java.time.LocalDateTime.now())
                .build();
    }

    private Maintenance maintenanceFor(Bus bus) {
        return Maintenance.builder()
                .id(1L)
                .bus(bus)
                .maintenanceType("OIL_CHANGE")
                .status(MaintenanceStatus.COMPLETED)
                .scheduledDate(LocalDate.now())
                .cost(100.0)
                .build();
    }

    // --- Refund report provider isolation ---

    @Test
    void provider1RefundReportContainsOnlyProvider1Refunds() {
        when(refundRepository.findByBooking_Schedule_Bus_ProviderId(1L))
                .thenReturn(List.of(refundFor(bus(1L, 1L))));

        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(1L);

        ReportResponse response = reportService.generateRefundReport(filters);

        assertThat(response.getData()).hasSize(1);
        verify_findAllNeverCalledForRefund();
    }

    @Test
    void provider2RefundReportContainsOnlyProvider2Refunds() {
        when(refundRepository.findByBooking_Schedule_Bus_ProviderId(2L))
                .thenReturn(List.of(refundFor(bus(2L, 2L))));

        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(2L);

        ReportResponse response = reportService.generateRefundReport(filters);

        assertThat(response.getData()).hasSize(1);
    }

    @Test
    void provider1CannotObtainProvider2RefundData() {
        // Provider 1's own scoped query never returns Provider 2's refunds -
        // the repository call itself is scoped to providerId=1.
        when(refundRepository.findByBooking_Schedule_Bus_ProviderId(1L)).thenReturn(List.of());

        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(1L);

        ReportResponse response = reportService.generateRefundReport(filters);

        assertThat(response.getData()).isEmpty();
        verify_findAllNeverCalledForRefund();
    }

    @Test
    void adminRefundReportWithoutProviderIdRemainsCrossProvider() {
        when(refundRepository.findAll()).thenReturn(List.of(refundFor(bus(1L, 1L)), refundFor(bus(2L, 2L))));

        ReportFilterRequest filters = new ReportFilterRequest(); // providerId left null - admin, no scope requested

        ReportResponse response = reportService.generateRefundReport(filters);

        assertThat(response.getData()).hasSize(2);
    }

    @Test
    void refundExportPathReceivesProviderScopedData() {
        // exportReportToCsv delegates through generateReport -> generateRefundReport,
        // so the same provider-scoping applies to the export path automatically.
        when(refundRepository.findByBooking_Schedule_Bus_ProviderId(1L))
                .thenReturn(List.of(refundFor(bus(1L, 1L))));
        when(reportExportUtil.generateCsv(any(), any())).thenReturn("csv-data");

        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(1L);

        String csv = reportService.exportReportToCsv(ReportType.REFUND, filters);

        assertThat(csv).isEqualTo("csv-data");
        verify_findAllNeverCalledForRefund();
    }

    // --- Maintenance report provider isolation ---

    @Test
    void provider1MaintenanceReportContainsOnlyProvider1Records() {
        when(maintenanceRepository.findByProviderId(1L)).thenReturn(List.of(maintenanceFor(bus(1L, 1L))));

        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(1L);

        ReportResponse response = reportService.generateMaintenanceReport(filters);

        assertThat(response.getData()).hasSize(1);
        verify_findAllNeverCalledForMaintenance();
    }

    @Test
    void provider2MaintenanceReportContainsOnlyProvider2Records() {
        when(maintenanceRepository.findByProviderId(2L)).thenReturn(List.of(maintenanceFor(bus(2L, 2L))));

        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(2L);

        ReportResponse response = reportService.generateMaintenanceReport(filters);

        assertThat(response.getData()).hasSize(1);
    }

    @Test
    void providerCannotUseAnotherProvidersBusIdForMaintenanceReport() {
        // Provider 1's own maintenance list never contains Bus 99 (Provider 2's
        // bus), so filtering that already-scoped list by busId=99 yields nothing.
        when(maintenanceRepository.findByProviderId(1L)).thenReturn(List.of(maintenanceFor(bus(1L, 1L))));

        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(1L);
        filters.setBusId(99L); // Provider 2's bus

        ReportResponse response = reportService.generateMaintenanceReport(filters);

        assertThat(response.getData()).isEmpty();
    }

    @Test
    void noBusIdMaintenanceReportRemainsProviderScoped() {
        when(maintenanceRepository.findByProviderId(1L)).thenReturn(List.of(maintenanceFor(bus(1L, 1L))));

        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(1L);

        ReportResponse response = reportService.generateMaintenanceReport(filters);

        assertThat(response.getData()).hasSize(1);
        verify_findAllNeverCalledForMaintenance();
    }

    @Test
    void adminMaintenanceReportWithoutProviderIdRemainsCrossProvider() {
        when(maintenanceRepository.findAll())
                .thenReturn(List.of(maintenanceFor(bus(1L, 1L)), maintenanceFor(bus(2L, 2L))));

        ReportFilterRequest filters = new ReportFilterRequest(); // providerId left null - admin

        ReportResponse response = reportService.generateMaintenanceReport(filters);

        assertThat(response.getData()).hasSize(2);
    }

    private void verify_findAllNeverCalledForRefund() {
        org.mockito.Mockito.verify(refundRepository, never()).findAll();
    }

    private void verify_findAllNeverCalledForMaintenance() {
        org.mockito.Mockito.verify(maintenanceRepository, never()).findAll();
    }
}
