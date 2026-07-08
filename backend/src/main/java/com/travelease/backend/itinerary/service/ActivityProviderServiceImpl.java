package com.travelease.backend.itinerary.service;

import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.itinerary.dto.ActivityProviderRequest;
import com.travelease.backend.itinerary.dto.ActivityProviderResponse;
import com.travelease.backend.itinerary.dto.ActivitySlotRequest;
import com.travelease.backend.itinerary.dto.ActivitySlotResponse;
import com.travelease.backend.itinerary.entity.Activity;
import com.travelease.backend.itinerary.entity.ActivityBookingStatus;
import com.travelease.backend.itinerary.entity.ActivitySlot;
import com.travelease.backend.itinerary.repository.ActivityBookingRepository;
import com.travelease.backend.itinerary.repository.ActivityRepository;
import com.travelease.backend.itinerary.repository.ActivitySlotRepository;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityProviderServiceImpl implements ActivityProviderService {

    private static final List<ActivityBookingStatus> CAPACITY_CONSUMING_STATUSES =
            List.of(ActivityBookingStatus.CONFIRMED, ActivityBookingStatus.ATTENDED, ActivityBookingStatus.NO_SHOW);

    private final ActivityRepository activityRepository;
    private final ActivitySlotRepository activitySlotRepository;
    private final ActivityBookingRepository activityBookingRepository;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public ActivityProviderResponse createActivity(ActivityProviderRequest request) {
        Long effectiveProviderId = securityUtil.resolveEffectiveActivityProviderId(request.providerId());
        if (effectiveProviderId == null) {
            throw new InvalidRequestException("providerId is required to create an activity");
        }

        Activity activity = new Activity();
        activity.setActivityId(UUID.randomUUID().toString());
        activity.setProviderId(effectiveProviderId);
        applyRequest(activity, request);
        return toResponse(activityRepository.save(activity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityProviderResponse> getProviderActivities(Long providerId) {
        Long effectiveProviderId = securityUtil.resolveEffectiveActivityProviderId(providerId);
        List<Activity> activities = effectiveProviderId != null
                ? activityRepository.findByProviderId(effectiveProviderId)
                : activityRepository.findAll();
        return activities.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityProviderResponse getProviderActivity(String activityId) {
        Activity activity = getActivity(activityId);
        assertOwnsActivity(activity);
        return toResponse(activity);
    }

    @Override
    @Transactional
    public ActivityProviderResponse updateActivity(String activityId, ActivityProviderRequest request) {
        Activity activity = getActivity(activityId);
        assertOwnsActivity(activity);
        applyRequest(activity, request);
        return toResponse(activityRepository.save(activity));
    }

    @Override
    @Transactional
    public ActivitySlotResponse createSlot(String activityId, ActivitySlotRequest request) {
        Activity activity = getActivity(activityId);
        assertOwnsActivity(activity);

        ActivitySlot slot = new ActivitySlot();
        slot.setActivity(activity);
        applySlotRequest(slot, request);
        return toSlotResponse(activitySlotRepository.save(slot));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivitySlotResponse> getSlots(String activityId) {
        Activity activity = getActivity(activityId);
        assertOwnsActivity(activity);
        return activitySlotRepository.findByActivity_ActivityId(activityId).stream()
                .map(this::toSlotResponse)
                .toList();
    }

    @Override
    @Transactional
    public ActivitySlotResponse updateSlot(String activityId, UUID slotId, ActivitySlotRequest request) {
        Activity activity = getActivity(activityId);
        assertOwnsActivity(activity);

        ActivitySlot slot = activitySlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity slot with id " + slotId + " not found"));
        if (!slot.getActivity().getActivityId().equals(activityId)) {
            throw new InvalidRequestException("Slot does not belong to activity " + activityId);
        }

        int bookedParticipants = activityBookingRepository.sumParticipantsByActivitySlotIdAndStatusIn(
                slot.getId(), CAPACITY_CONSUMING_STATUSES);

        if (bookedParticipants > 0) {
            if (!request.activityDate().equals(slot.getActivityDate())
                    || !request.startTime().equals(slot.getStartTime())
                    || !request.endTime().equals(slot.getEndTime())) {
                throw new InvalidRequestException(
                        "Slot date/time cannot be changed once it has active bookings");
            }
            if (request.capacity() < bookedParticipants) {
                throw new InvalidRequestException(
                        "Capacity cannot be reduced below the " + bookedParticipants + " already-booked participants");
            }
        }

        applySlotRequest(slot, request);
        return toSlotResponse(activitySlotRepository.save(slot));
    }

    private void applyRequest(Activity activity, ActivityProviderRequest request) {
        activity.setDestinationId(request.destinationId());
        activity.setActivityName(request.activityName());
        activity.setDurationHours(request.durationHours());
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
        activity.setDescription(request.description());
    }

    private void applySlotRequest(ActivitySlot slot, ActivitySlotRequest request) {
        slot.setActivityDate(request.activityDate());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setPrice(request.price());
        slot.setCapacity(request.capacity());
    }

    private Activity getActivity(String activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity with id " + activityId + " not found"));
    }

    /**
     * Role gate ("is this caller an Activity Provider/Admin?") is enforced
     * declaratively via @PreAuthorize on ActivityProviderController. This
     * asserts the second, required axis: "does this Activity Provider own this
     * Activity resource?" - mirrors AccommodationServiceImpl.assertOwnsHotel.
     */
    private void assertOwnsActivity(Activity activity) {
        securityUtil.resolveEffectiveActivityProviderId(activity.getProviderId());
    }

    private ActivityProviderResponse toResponse(Activity activity) {
        return new ActivityProviderResponse(
                activity.getActivityId(),
                activity.getProviderId(),
                activity.getDestinationId(),
                activity.getActivityName(),
                activity.getDurationHours(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getDescription()
        );
    }

    private ActivitySlotResponse toSlotResponse(ActivitySlot slot) {
        int bookedParticipants = activityBookingRepository.sumParticipantsByActivitySlotIdAndStatusIn(
                slot.getId(), CAPACITY_CONSUMING_STATUSES);
        return new ActivitySlotResponse(
                slot.getId(),
                slot.getActivity().getActivityId(),
                slot.getActivityDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getPrice(),
                slot.getCapacity(),
                slot.getCapacity() - bookedParticipants
        );
    }
}
