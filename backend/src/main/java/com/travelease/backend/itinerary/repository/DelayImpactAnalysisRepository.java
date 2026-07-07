package com.travelease.backend.itinerary.repository;

import com.travelease.backend.itinerary.entity.DelayImpactAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DelayImpactAnalysisRepository
        extends JpaRepository<DelayImpactAnalysis, String> {

    List<DelayImpactAnalysis> findByDelayId(String delayId);

    List<DelayImpactAnalysis> findByAffectedActivityId(
            String affectedActivityId);
}