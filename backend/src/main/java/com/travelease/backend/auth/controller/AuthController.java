package com.travelease.backend.auth.controller;

import com.travelease.backend.auth.dto.LoginRequest;
import com.travelease.backend.auth.dto.LoginResponse;
import com.travelease.backend.auth.dto.RegisterRequest;
import com.travelease.backend.auth.dto.UserResponse;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.service.AuthService;
import com.travelease.backend.auth.service.UserService;
import com.travelease.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Public login and registration endpoints plus the current-user lookup")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "ACCESS: PUBLIC\nSCOPE: Creates a new user account.\nIDENTITY: No JWT is required.")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password", description = "ACCESS: PUBLIC\nSCOPE: Authenticates credentials and returns the login response.\nIDENTITY: No JWT is required.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(loginResponse, "Login successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user", description = "ACCESS: AUTHENTICATED\nSCOPE: Returns the user resolved from the current JWT identity.\nIDENTITY: The authenticated email from the security context is used; no client-supplied userId is trusted.")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        User user = userService.getByEmail(authentication.getName());
        UserResponse response = new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name(), user.getProviderId()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Current user retrieved"));
    }
}
