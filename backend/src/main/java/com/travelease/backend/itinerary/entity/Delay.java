package com.travelease.backend.itinerary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "delays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Delay {

    @Id
    @Column(name = "DelayID")
    private String delayId;

    @Column(name = "TripID", nullable = false)
    private String tripId;

    @Column(name = "ReportedBy", nullable = false)
    private String reportedBy;

    @Column(name = "DelayMinutes")
    private Integer delayMinutes;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;
}