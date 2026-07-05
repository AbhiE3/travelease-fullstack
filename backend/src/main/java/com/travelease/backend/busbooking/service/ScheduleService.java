package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.BusSearchCriteriaRequest;
import com.travelease.backend.busbooking.dto.request.ScheduleRequest;
import com.travelease.backend.busbooking.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {

    ScheduleResponse createSchedule(ScheduleRequest request);

    ScheduleResponse updateSchedule(Long id, ScheduleRequest request);

    void deleteSchedule(Long id);

    ScheduleResponse getScheduleById(Long id);

    List<ScheduleResponse> getAllSchedules();

    List<BusSearchResponse> searchBuses(String source, String destination, LocalDate travelDate);

    PaginatedSearchResponse<SmartSearchResponse> smartSearch(BusSearchCriteriaRequest criteria);

    List<PopularRouteResponse> getPopularRoutes(int limit);

    List<SearchHistoryResponse> getSearchHistory(Pageable pageable);

    List<SearchSuggestionResponse> getSearchSuggestions(int limit);

    List<PopularRouteResponse> getFrequentlyBookedRoutes(int limit);
}
