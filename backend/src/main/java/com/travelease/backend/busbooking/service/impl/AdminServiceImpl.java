package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.response.BookingResponse;
import com.travelease.backend.busbooking.dto.response.DashboardStatsResponse;
import com.travelease.backend.busbooking.mapper.BookingMapper;
import com.travelease.backend.busbooking.repository.BookingRepository;
import com.travelease.backend.busbooking.repository.BusRepository;
import com.travelease.backend.busbooking.repository.BusScheduleRepository;
import com.travelease.backend.busbooking.repository.RouteRepository;
import com.travelease.backend.busbooking.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final BookingRepository bookingRepository;
    private final BusScheduleRepository scheduleRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalBuses = busRepository.countByStatusTrue();
        long totalRoutes = routeRepository.count();
        long totalBookings = bookingRepository.count();
        Double totalRevenue = bookingRepository.sumRevenueFromConfirmedBookings();
        long todayBookings = bookingRepository.countTodayBookings(LocalDate.now());
        long activeSchedules = scheduleRepository.countActiveSchedules();

        return DashboardStatsResponse.builder()
                .totalBuses(totalBuses)
                .totalRoutes(totalRoutes)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .todayBookings(todayBookings)
                .activeSchedules(activeSchedules)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getScheduleBookings(Long scheduleId) {
        return bookingRepository.findByScheduleId(scheduleId)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }
}
