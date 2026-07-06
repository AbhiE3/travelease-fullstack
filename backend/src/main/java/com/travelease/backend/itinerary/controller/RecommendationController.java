package com.travelease.backend.itinerary.controller;

import com.travelease.backend.itinerary.entity.Recommendation;
import com.travelease.backend.itinerary.repository.RecommendationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "http://localhost:4200")
public class RecommendationController {

    @Autowired
    private RecommendationRepository recommendationRepository;

    // US-REC-01, US-23 — GET /api/recommendations?categoryId=
    // Get personalized suggestions by traveler category
    // 1=Solo 2=Couple 3=Family 4=Friends 5=Corporate
    @GetMapping
    public ResponseEntity<List<Recommendation>> getRecommendations(
            @RequestParam Integer categoryId) {
        List<Recommendation> recommendations =
                recommendationRepository
                        .findByCategoryIdOrderByRankOrderAsc(
                                categoryId);
        return ResponseEntity.ok(recommendations);
    }
}



