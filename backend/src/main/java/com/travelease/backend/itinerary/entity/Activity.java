package com.travelease.backend.itinerary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    @Id
    @Column(name = "ActivityID")
    private String activityId;

    /**
     * The Activity Provider tenant that owns this activity. Assigned server-side
     * from the authenticated ROLE_ACTIVITY_PROVIDER account (or explicitly by
     * ROLE_ADMIN) at creation time; never accepted as client-authoritative on
     * update. Mirrors Hotel.providerId's role for the accommodation domain.
     */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "DestinationID")
    private Integer destinationId;

    @Column(name = "ActivityName")
    private String activityName;

    @Column(name = "DurationHours")
    private Double durationHours;

    @Column(name = "StartTime")
    private String startTime;

    @Column(name = "EndTime")
    private String endTime;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;
}