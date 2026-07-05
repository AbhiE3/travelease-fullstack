package com.travelease.backend.itinerary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "delay_impact_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DelayImpactAnalysis {

    @Id
    @Column(name = "AnalysisID")
    private String analysisId;

    @Column(name = "DelayID", nullable = false)
    private String delayId;

    @Column(name = "AffectedActivityID", nullable = false)
    private String affectedActivityId;

    @Column(name = "SuggestedStartTime")
    private LocalDateTime suggestedStartTime;

    @Column(name = "SuggestedEndTime")
    private LocalDateTime suggestedEndTime;

    @Column(name = "UserAccepted")
    private Boolean userAccepted = false;
}