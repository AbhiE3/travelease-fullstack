package com.travelease.backend.itinerary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

    @Id
    @Column(name = "RecommendationID")
    private String recommendationId;

    @Column(name = "CategoryID")
    private Integer categoryId;

    @Column(name = "RecommendationType")
    private String recommendationType;  // Hotel, Attraction, Activity

    @Column(name = "ReferenceID")
    private String referenceId;

    @Column(name = "RankOrder")
    private Integer rankOrder;
}