package com.travelease.backend.itinerary.mapper;

import com.travelease.backend.itinerary.dto.ItineraryRequest;
import com.travelease.backend.itinerary.dto.ItineraryResponse;
import com.travelease.backend.itinerary.dto.NotificationResponse;
import com.travelease.backend.itinerary.entity.Itinerary;
import com.travelease.backend.itinerary.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class ItineraryMapper {

    // Convert ItineraryRequest (from Angular) → Itinerary Entity (to save in DB)
    public Itinerary toEntity(ItineraryRequest request) {
        Itinerary itinerary = new Itinerary();
        itinerary.setTripId(request.getTripId());
        itinerary.setActivityId(request.getActivityId());
        itinerary.setActivityDate(request.getActivityDate());
        itinerary.setStartTime(request.getStartTime());
        itinerary.setEndTime(request.getEndTime());
        itinerary.setStatus(request.getStatus() != null
                ? request.getStatus() : "Pending");
        return itinerary;
    }

    // Convert Itinerary Entity (from DB) → ItineraryResponse (to send to Angular)
    public ItineraryResponse toResponse(Itinerary itinerary) {
        ItineraryResponse response = new ItineraryResponse();
        response.setItineraryId(itinerary.getItineraryId());
        response.setTripId(itinerary.getTripId());
        response.setActivityId(itinerary.getActivityId());
        response.setActivityDate(itinerary.getActivityDate());
        response.setStartTime(itinerary.getStartTime());
        response.setEndTime(itinerary.getEndTime());
        response.setStatus(itinerary.getStatus());
        response.setCompletionTime(itinerary.getCompletionTime());
        return response;
    }

    // Convert Notification Entity (from DB) → NotificationResponse (to Angular)
    public NotificationResponse toNotificationResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notification.getNotificationId());
        response.setUserId(notification.getUserId());
        response.setNotificationType(notification.getNotificationType());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setIsRead(notification.getIsRead());
        response.setCreatedDate(notification.getCreatedDate());
        return response;
    }
}