package com.travelease.backend.health;

import com.travelease.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "System Health", description = "Public liveness check used to verify the backend is running")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "ACCESS: PUBLIC\nSCOPE: Returns a lightweight liveness response for monitoring and manual verification.\nIDENTITY: No JWT is required.")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"), "Backend is healthy");
    }
}
