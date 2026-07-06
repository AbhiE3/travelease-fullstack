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