package com.travelease.backend.itinerary.service;

import com.travelease.backend.itinerary.dto.ActivityProviderRequest;
import com.travelease.backend.itinerary.dto.ActivityProviderResponse;
import com.travelease.backend.itinerary.dto.ActivitySlotRequest;
import com.travelease.backend.itinerary.dto.ActivitySlotResponse;

import java.util.List;

public interface ActivityProviderService {

    ActivityProviderResponse createActivity(ActivityProviderRequest request);

    List<ActivityProviderResponse> getProviderActivities(Long providerId);

    ActivityProviderResponse getProviderActivity(String activityId);

    ActivityProviderResponse updateActivity(String activityId, ActivityProviderRequest request);

    ActivitySlotResponse createSlot(String activityId, ActivitySlotRequest request);

    List<ActivitySlotResponse> getSlots(String activityId);

    ActivitySlotResponse updateSlot(String activityId, java.util.UUID slotId, ActivitySlotRequest request);
}
