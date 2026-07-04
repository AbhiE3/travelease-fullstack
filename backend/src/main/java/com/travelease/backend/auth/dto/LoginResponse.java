package com.travelease.backend.auth.dto;

public record LoginResponse(String accessToken, UserResponse user) {
}
