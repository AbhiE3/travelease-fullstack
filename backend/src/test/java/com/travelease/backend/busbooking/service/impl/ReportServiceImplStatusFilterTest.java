package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.ReportFilterRequest;
import com.travelease.backend.busbooking.dto.response.ReportResponse;
import com.travelease.backend.busbooking.entity.Booking;
import com.travelease.backend.busbooking.entity.BookingSeat;
import com.travelease.backend.busbooking.entity.BusSchedule;
import com.travelease.backend.busbooking.entity.Route;
import com.travelease.backend.busbooking.entity.Bus;
import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import com.travelease.backend.busbooking.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplStatusFilterTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void bookingReportIncludesCompletedBookingsInRevenueAndPassengerTotals() {
        Booking confirmed = bookingWith(BookingStatus.CONFIRMED, 500.0);
        Booking completed = bookingWith(BookingStatus.COMPLETED, 300.0);
        when(bookingRepository.findAll(any(Specification.class))).thenReturn(List.of(confirmed, completed));

        ReportResponse response = reportService.generateBookingReport(new ReportFilterRequest());

        assertThat(response.getSummary().getTotalRevenue()).isEqualTo(800.0);
        assertThat(response.getSummary().getTotalPassengers()).isEqualTo(2L);
    }

    @Test
    void revenueReportRejectsOutOfDomainStatusFilter() {
        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setBookingStatus(BookingStatus.PENDING);

        assertThatThrownBy(() -> reportService.generateRevenueReport(filters))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancellationReportRejectsNonCancelledStatusFilter() {
        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setBookingStatus(BookingStatus.CONFIRMED);

        assertThatThrownBy(() -> reportService.generateCancellationReport(filters))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Booking bookingWith(BookingStatus status, double fare) {
        Route route = Route.builder().id(1L).source("City A").destination("City B").build();
        Bus bus = Bus.builder().id(1L).busNumber("BUS-1").providerId(1L).build();
        BusSchedule schedule = BusSchedule.builder().id(1L).route(route).bus(bus)
                .travelDate(java.time.LocalDate.now()).build();
        return Booking.builder()
                .id(1L)
                .userId(java.util.UUID.randomUUID())
                .schedule(schedule)
                .status(status)
                .totalFare(fare)
                .bookingSeats(List.of(BookingSeat.builder().build()))
                .bookedAt(java.time.LocalDateTime.now())
                .build();
    }
}
