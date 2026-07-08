package com.travelease.backend.itinerary.controller;

import com.travelease.backend.itinerary.entity.Recommendation;
import com.travelease.backend.itinerary.repository.RecommendationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Recommendations", description = "Public traveler recommendations by category")
public class RecommendationController {

    @Autowired
    private RecommendationRepository recommendationRepository;

    // US-REC-01, US-23 — GET /api/recommendations?categoryId=
    // Get personalized suggestions by traveler category
    // 1=Solo 2=Couple 3=Family 4=Friends 5=Corporate
    @GetMapping
    @Operation(summary = "Get traveler recommendations", description = "ACCESS: PUBLIC\nSCOPE: Returns recommendation records for the requested traveler categoryId.\nIDENTITY: No JWT is required.")
    public ResponseEntity<List<Recommendation>> getRecommendations(
            @RequestParam Integer categoryId) {
        List<Recommendation> recommendations =
                recommendationRepository
                        .findByCategoryIdOrderByRankOrderAsc(
                                categoryId);
        return ResponseEntity.ok(recommendations);
    }
}



