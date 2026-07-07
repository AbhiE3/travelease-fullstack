package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.response.BookingResponse;
import com.travelease.backend.busbooking.dto.response.DashboardStatsResponse;

import java.util.List;

public interface AdminService {

    DashboardStatsResponse getDashboardStats();

    List<BookingResponse> getScheduleBookings(Long scheduleId);
}
