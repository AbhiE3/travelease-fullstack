package com.travelease.backend.itinerary.repository;

import com.travelease.backend.itinerary.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecommendationRepository
        extends JpaRepository<Recommendation, String> {

    List<Recommendation> findByCategoryIdOrderByRankOrderAsc(
            Integer categoryId);
}