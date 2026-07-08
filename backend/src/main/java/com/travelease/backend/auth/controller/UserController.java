package com.travelease.backend.auth.controller;

import com.travelease.backend.auth.dto.UserResponse;
import com.travelease.backend.auth.service.UserService;
import com.travelease.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Public login and registration endpoints plus the current-user lookup")
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    @Operation(summary = "Search registered travelers by name or email", description = "ACCESS: AUTHENTICATED\nSCOPE: Matches ROLE_TRAVELER users whose name or email contains the query, capped at 10 results.\nIDENTITY: Any authenticated user may search, e.g. to find a traveler to invite to a trip.")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(@RequestParam(name = "q") String query) {
        List<UserResponse> response = userService.searchTravelers(query);
        return ResponseEntity.ok(ApiResponse.success(response, "Users retrieved"));
    }
}
